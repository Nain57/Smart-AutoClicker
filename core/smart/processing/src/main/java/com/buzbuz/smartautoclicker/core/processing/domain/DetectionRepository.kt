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
package com.buzbuz.smartautoclicker.core.processing.domain

import android.content.Context
import android.content.Intent
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.DetectorEngine
import com.buzbuz.smartautoclicker.core.processing.data.DetectorState
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ActionTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ActionTryListener
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageConditionProcessingTryListener
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageConditionTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageEventProcessingTryListener
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageEventTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ScenarioTry
import com.buzbuz.smartautoclicker.core.smart.training.TrainingRepository
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextData

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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DetectionRepository @Inject constructor(
    @Dispatcher(Main) mainDispatcher: CoroutineDispatcher,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val scenarioRepository: IRepository,
    private val detectorEngine: DetectorEngine,
    private val trainingDataRepository: TrainingRepository,
): Dumpable {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + mainDispatcher)
    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Interacts with the OS to execute the actions */
    private var actionExecutor: SmartActionExecutor? = null

    private var projectionErrorHandler: (() -> Unit)? = null

    /** Stop the detection automatically after selected delay */
    private var autoStopJob: Job? = null

    /** The current scenario unique identifier. */
    private val _scenarioId: MutableStateFlow<Identifier?> = MutableStateFlow(null)
    val scenarioId: StateFlow<Identifier?> = _scenarioId

    /** The state of the scenario processing. */
    val detectionState: Flow<DetectionState> = detectorEngine.state
        .mapNotNull { it.toDetectionState() }

    /**
     * Tells if the detection can be started or not.
     * It requires at least one event enabled on start to be started.
     */
    val canStartDetection: Flow<Boolean> = scenarioId
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

    fun setScenarioId(identifier: Identifier, markAsUsed: Boolean = false) {
        _scenarioId.value = identifier

        if (markAsUsed) {
            coroutineScopeIo.launch { scenarioRepository.markAsUsed(identifier) }
        }
    }

    fun setExecutor(androidExecutor: SmartActionExecutor) {
        actionExecutor = androidExecutor
    }

    fun setProjectionErrorHandler(handler: () -> Unit) {
        projectionErrorHandler = handler
    }

    fun getScenarioId(): Identifier? = _scenarioId.value

    fun isRunning(): Boolean =
        detectorEngine.state.value == DetectorState.DETECTING

    fun startScreenRecord(context: Context, resultCode: Int, data: Intent) {
        actionExecutor?.let { executor ->
            detectorEngine.startScreenRecord(context, resultCode, data, executor) {
                coroutineScopeMain.launch { projectionErrorHandler?.invoke() }
            }
        }
    }

    suspend fun startDetection(context: Context, progressListener: ScenarioProcessingListener?, autoStopDuration: Duration? = null) {
        val id = scenarioId.value?.databaseId ?: return
        val scenario = scenarioRepository.getScenario(id) ?: return
        val events = scenarioRepository.getImageEvents(id)
        val triggerEvents = scenarioRepository.getTriggerEvents(id)
        val trainedTextData = events.getRequiredTrainedTextData() ?: return

        detectorEngine.startDetection(
            context = context,
            scenario = scenario,
            imageEvents = events,
            triggerEvents = triggerEvents,
            trainedTextData = trainedTextData,
            bitmapSupplier = scenarioRepository::getConditionBitmap,
            progressListener = progressListener,
        )

        autoStopDuration?.let { duration ->
            autoStopJob?.cancel()
            autoStopJob = coroutineScopeIo.launch {
                delay(duration)
                stopDetection()
            }
        }
    }

    fun stopDetection() {
        detectorEngine.stopDetection()
        autoStopJob?.cancel()
        autoStopJob = null
    }

    fun stopScreenRecord() {
        projectionErrorHandler = null

        detectorEngine.apply {
            stopScreenRecord()
            clear()
        }

        actionExecutor = null
        _scenarioId.value = null
    }

    fun tryEvent(context: Context, scenario: Scenario, event: ImageEvent, listener: (IConditionsResult) -> Unit) {
        val triedElement = ImageEventTry(scenario, event)
        tryElement(
            context,
            triedElement,
            ImageEventProcessingTryListener(triedElement, listener)
        )
    }

    fun tryImageCondition(
        context: Context,
        scenario: Scenario,
        condition: ImageCondition,
        listener: (ScreenConditionResult) -> Unit,
    ) {
        val triedElement = ImageConditionTry(scenario, condition)
        tryElement(
            context = context,
            elementTry = triedElement,
            listener = ImageConditionProcessingTryListener(triedElement, listener)
        )
    }

    fun tryAction(context: Context,  scenario: Scenario, action: Action, listener: () -> Unit) {
        tryElement(
            context = context,
            elementTry = ActionTry(scenario, action),
            listener = ActionTryListener(listener),
        )
    }

    private fun tryElement(context: Context, elementTry: ScenarioTry, listener: ScenarioProcessingListener) {
        Log.d(TAG, "Trying element: Scenario=${elementTry.scenario}; ImageEvents=${elementTry.imageEvents}")
        val trainedTextData = elementTry.imageEvents.getRequiredTrainedTextData() ?: return

        detectorEngine.startDetection(
            context = context,
            scenario = elementTry.scenario,
            imageEvents = elementTry.imageEvents,
            triggerEvents = elementTry.triggerEvents,
            trainedTextData = trainedTextData,
            bitmapSupplier = scenarioRepository::getConditionBitmap,
            progressListener = listener,
        )
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* DetectionRepository:")

            append(contentPrefix)
                .append("- scenarioId=${scenarioId.value}; ")
                .append("canStartDetection=${canStartDetection.dumpWithTimeout() ?: false}; ")
                .append("detectionState=${detectionState.dumpWithTimeout() ?: DetectionState.INACTIVE}; ")
                .println()
        }
    }

    private fun List<ImageEvent>.getRequiredTrainedTextData(): TrainedTextData? {
        val requiredLanguages = buildSet {
            this@getRequiredTrainedTextData.forEach { screenEvent ->
                screenEvent.conditions.forEach { screenCondition ->
                    if (screenCondition is TextCondition) {
                        add(screenCondition.textLanguage)
                    }
                }
            }
        }

        return trainingDataRepository.getTrainedTextDataForLanguages(requiredLanguages)
    }
}

private const val TAG = "DetectionRepository"

/** The minimum detection quality for the algorithm. */
const val DETECTION_QUALITY_MIN = com.buzbuz.smartautoclicker.core.detection.DETECTION_QUALITY_MIN