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

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.DetectorEngine
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageConditionProcessingTryListener
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageConditionTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageEventProcessingTryListener
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ImageEventTry
import com.buzbuz.smartautoclicker.core.processing.domain.trying.ScenarioTry

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DetectionRepository @Inject constructor(
    private val scenarioRepository: IRepository,
    private val detectorEngine: DetectorEngine,
): Dumpable {

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

    fun setScenarioId(identifier: Identifier) {
        _scenarioId.value = identifier
    }

    fun getScenarioId(): Identifier? = _scenarioId.value

    fun startScreenRecord(
        context: Context,
        resultCode: Int,
        data: Intent,
        androidExecutor: AndroidExecutor,
    ) {
        detectorEngine.startScreenRecord(context, resultCode, data, androidExecutor)
    }

    suspend fun startDetection(context: Context, progressListener: ScenarioProcessingListener) {
        val id = scenarioId.value?.databaseId ?: return
        val scenario = scenarioRepository.getScenario(id) ?: return
        val events = scenarioRepository.getImageEvents(id)
        val triggerEvents = scenarioRepository.getTriggerEvents(id)

        detectorEngine.startDetection(
            context = context,
            scenario = scenario,
            imageEvents = events,
            triggerEvents = triggerEvents,
            bitmapSupplier = scenarioRepository::getConditionBitmap,
            progressListener = progressListener,
        )
    }

    fun stopDetection() {
        detectorEngine.stopDetection()
    }

    fun stopScreenRecord() {
        detectorEngine.apply {
            stopScreenRecord()
            clear()
        }

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
        listener: (ImageConditionResult) -> Unit,
    ) {
        val triedElement = ImageConditionTry(scenario, condition)
        tryElement(
            context = context,
            elementTry = triedElement,
            listener = ImageConditionProcessingTryListener(triedElement, listener)
        )
    }

    private fun tryElement(context: Context, elementTry: ScenarioTry, listener: ScenarioProcessingListener) {
        Log.d(TAG, "Trying element: Scenario=${elementTry.scenario}; ImageEvents=${elementTry.imageEvents}")
        detectorEngine.startDetection(
            context = context,
            scenario = elementTry.scenario,
            imageEvents = elementTry.imageEvents,
            triggerEvents = elementTry.triggerEvents,
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
}

private const val TAG = "DetectionRepository"

/** The maximum detection quality for the algorithm. */
const val DETECTION_QUALITY_MAX = com.buzbuz.smartautoclicker.core.detection.DETECTION_QUALITY_MAX
/** The minimum detection quality for the algorithm. */
const val DETECTION_QUALITY_MIN = com.buzbuz.smartautoclicker.core.detection.DETECTION_QUALITY_MIN