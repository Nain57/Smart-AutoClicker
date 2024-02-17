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
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.Repository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class DetectionRepository private constructor(context: Context) {

    companion object {

        /** Singleton preventing multiple instances of the DebuggingRepository at the same time. */
        @Volatile
        private var INSTANCE: DetectionRepository? = null

        /**
         * Get the DetectionRepository singleton, or instantiates it if it wasn't yet.
         *
         * @return the DetectionRepository singleton.
         */
        fun getDetectionRepository(context: Context): DetectionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DetectionRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }

    /** Repository providing data for the scenario. */
    private val scenarioRepository = Repository.getRepository(context)

    /** Engine controlling the screen recording and the scenario processing. */
    private val detectorEngine: MutableStateFlow<DetectorEngine?> = MutableStateFlow(null)
    /** The current scenario unique identifier. */
    private val _scenarioId: MutableStateFlow<Identifier?> = MutableStateFlow(null)
    val scenarioId: StateFlow<Identifier?> = _scenarioId

    /** The state of the scenario processing. */
    val detectionState: Flow<DetectionState> = detectorEngine.flatMapLatest { engine ->
        engine?.state?.mapNotNull { it.toDetectionState() }
            ?: flowOf(DetectionState.INACTIVE)
    }

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
        detectorEngine.value = DetectorEngine(context).also { newEngine ->
            newEngine.startScreenRecord(context, resultCode, data, androidExecutor)
        }
    }

    suspend fun startDetection(context: Context, progressListener: ScenarioProcessingListener) {
        val id = scenarioId.value?.databaseId ?: return
        val scenario = scenarioRepository.getScenario(id) ?: return
        val events = scenarioRepository.getImageEvents(id)
        val triggerEvents = scenarioRepository.getTriggerEvents(id)

        detectorEngine.value?.startDetection(
            context = context,
            scenario = scenario,
            imageEvents = events,
            triggerEvents = triggerEvents,
            bitmapSupplier = scenarioRepository::getConditionBitmap,
            progressListener = progressListener,
        )
    }

    fun stopDetection() {
        detectorEngine.value?.stopDetection()
    }

    fun stopScreenRecord() {
        detectorEngine.value?.apply {
            stopScreenRecord()
            clear()
        }
        detectorEngine.value = null
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
        detectorEngine.value?.startDetection(
            context = context,
            scenario = elementTry.scenario,
            imageEvents = elementTry.imageEvents,
            triggerEvents = elementTry.triggerEvents,
            bitmapSupplier = scenarioRepository::getConditionBitmap,
            progressListener = listener,
        )
    }
}

private const val TAG = "DetectionRepository"

/** The maximum detection quality for the algorithm. */
const val DETECTION_QUALITY_MAX = com.buzbuz.smartautoclicker.core.detection.DETECTION_QUALITY_MAX
/** The minimum detection quality for the algorithm. */
const val DETECTION_QUALITY_MIN = com.buzbuz.smartautoclicker.core.detection.DETECTION_QUALITY_MIN