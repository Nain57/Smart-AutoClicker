/*
 * Copyright (C) 2024 Kevin Buzeau
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
@file:JvmName("SmartProcessingRepositoryKt")

package com.buzbuz.smartautoclicker.core.processing.domain

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.DetectorEngine
import com.buzbuz.smartautoclicker.core.processing.data.DetectorState
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.processing.domain.model.toDetectionState
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ActionTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageConditionTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageEventTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ScenarioTry

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
internal class SmartProcessingRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(Main) mainDispatcher: CoroutineDispatcher,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val scenarioRepository: IRepository,
    private val detectorEngine: DetectorEngine,
): SmartProcessingRepository {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + mainDispatcher)
    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    private val wakeLock: PowerManager.WakeLock =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).let { powerManager ->
            @Suppress("DEPRECATION") // Deprecated except for use case without a layout
            powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Klickr::Detection").apply {
                setReferenceCounted(false)
            }
        }

    private var projectionErrorHandler: (() -> Unit)? = null

    /** Stop the detection automatically after selected delay */
    private var autoStopJob: Job? = null

    private val _scenarioId: MutableStateFlow<Identifier?> = MutableStateFlow(null)
    override val scenarioId: StateFlow<Identifier?> = _scenarioId

    override val detectionState: Flow<DetectionState> = detectorEngine.state
        .mapNotNull { it.toDetectionState() }

    private val shouldKeepScreenOn: Flow<Boolean> = _scenarioId
        .combine(detectionState) { id, state ->
            id ?: return@combine false
            val scenario = scenarioRepository.getScenario(id.databaseId) ?: return@combine false

            state == DetectionState.DETECTING && scenario.keepScreenOn
        }
        .distinctUntilChanged()

    override val canStartDetection: Flow<Boolean> = scenarioId
        .filterNotNull()
        .flatMapLatest { scenarioRepository.getEventsFlow(it.databaseId) }
        .combine(detectionState) { events, state ->
            if (state == DetectionState.INACTIVE || state == DetectionState.ERROR_NO_NATIVE_LIB)
                return@combine false

            events.forEach {
                event -> if (event.enabledOnStart) return@combine true
            }
            false
        }


    init {
        shouldKeepScreenOn.onEach(::updateWakeLockState).launchIn(coroutineScopeIo)
    }

    override fun setScenarioId(identifier: Identifier, markAsUsed: Boolean) {
        _scenarioId.value = identifier

        if (markAsUsed) {
            coroutineScopeIo.launch { scenarioRepository.markAsUsed(identifier) }
        }
    }

    override fun setProjectionErrorHandler(handler: () -> Unit) {
        projectionErrorHandler = handler
    }

    override fun getScenarioId(): Identifier? = _scenarioId.value

    override fun isRunning(): Boolean =
        detectorEngine.state.value == DetectorState.DETECTING

    override fun startScreenRecord(resultCode: Int, data: Intent) {
        detectorEngine.startScreenRecord(resultCode, data) {
            coroutineScopeMain.launch { projectionErrorHandler?.invoke() }
        }
    }

    override suspend fun startDetection(context: Context, autoStopDuration: Duration?) {
        val id = scenarioId.value?.databaseId ?: return
        val scenario = scenarioRepository.getScenario(id) ?: return
        val events = scenarioRepository.getImageEvents(id)
        val triggerEvents = scenarioRepository.getTriggerEvents(id)

        detectorEngine.startDetection(
            context = context,
            scenario = scenario,
            imageEvents = events,
            triggerEvents = triggerEvents,
            isATry = false,
        )

        autoStopDuration?.let { duration ->
            autoStopJob?.cancel()
            autoStopJob = coroutineScopeIo.launch {
                delay(duration)
                stopDetection()
            }
        }
    }

    override fun stopDetection() {
        detectorEngine.stopDetection()
        autoStopJob?.cancel()
        autoStopJob = null
    }

    override fun stopScreenRecord() {
        projectionErrorHandler = null

        detectorEngine.apply {
            stopScreenRecord()
            clear()
        }

        _scenarioId.value = null
    }

    override fun tryEvent(context: Context, scenario: Scenario, event: ImageEvent) {
        val triedElement = ImageEventTry(scenario, event)
        tryElement(
            context,
            triedElement,
        )
    }

    override fun tryImageCondition(context: Context, scenario: Scenario, condition: ImageCondition) {
        val triedElement = ImageConditionTry(scenario, condition)
        tryElement(
            context = context,
            elementTry = triedElement,
        )
    }

    override fun tryAction(context: Context,  scenario: Scenario, action: Action) {
        tryElement(
            context = context,
            elementTry = ActionTry(scenario, action),
        )
    }

    private fun tryElement(context: Context, elementTry: ScenarioTry) {
        Log.d(TAG, "Trying element: Scenario=${elementTry.scenario}; ImageEvents=${elementTry.imageEvents}")
        detectorEngine.startDetection(
            context = context,
            scenario = elementTry.scenario,
            imageEvents = elementTry.imageEvents,
            triggerEvents = elementTry.triggerEvents,
            isATry = true,
        )
    }

    @SuppressLint("WakelockTimeout")
    private fun updateWakeLockState(keepScreenOn: Boolean) {
        Log.i(TAG, "updateWakeLockState: keepScreenOn=$keepScreenOn")
        if (keepScreenOn) wakeLock.acquire()
        else wakeLock.release()
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* SmartProcessingRepository:")

            append(contentPrefix)
                .append("- scenarioId=${scenarioId.value}; ")
                .append("canStartDetection=${canStartDetection.dumpWithTimeout() ?: false}; ")
                .append("detectionState=${detectionState.dumpWithTimeout() ?: DetectionState.INACTIVE}; ")
                .println()
        }
    }
}

private const val TAG = "SmartProcessingRepository"

/** The minimum detection quality for the algorithm. */
const val DETECTION_QUALITY_MIN = com.buzbuz.smartautoclicker.core.detection.DETECTION_QUALITY_MIN