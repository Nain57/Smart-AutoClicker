/*
 * Copyright (C) 2023 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.processing.data.AndroidExecutor
import com.buzbuz.smartautoclicker.core.processing.data.DetectorEngine
import com.buzbuz.smartautoclicker.core.processing.data.DetectorState
import com.buzbuz.smartautoclicker.core.processing.data.processor.ProgressListener

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val scenarioId = MutableStateFlow<Long?>(null)

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
        .map { id ->
            scenarioRepository.getCompleteEventList(id).forEach { event -> if (event.enabledOnStart) return@map true }
            false
        }

    fun startScreenRecord(
        context: Context,
        resultCode: Int,
        data: Intent,
        androidExecutor: AndroidExecutor,
        scenarioDbId: Long,
    ) {
        scenarioId.value = scenarioDbId
        detectorEngine.value = DetectorEngine(context).also { newEngine ->
            newEngine.startScreenRecord(context, resultCode, data, androidExecutor)
        }
    }

    suspend fun startDetection(progressListener: ProgressListener) {
        val id = scenarioId.value ?: return
        val scenario = scenarioRepository.getScenario(id)
        val events = scenarioRepository.getCompleteEventList(id)
        val endCondition = scenarioRepository.getEndConditions(id)

        detectorEngine.value?.startDetection(
            scenario = scenario,
            events = events,
            endConditions = endCondition,
            bitmapSupplier = scenarioRepository::getBitmap,
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
    }

    private fun DetectorState.toDetectionState(): DetectionState? = when (this) {
        DetectorState.CREATED -> DetectionState.INACTIVE
        DetectorState.RECORDING -> DetectionState.RECORDING
        DetectorState.DETECTING -> DetectionState.DETECTING
        DetectorState.DESTROYED -> DetectionState.INACTIVE
        DetectorState.TRANSITIONING -> null // Return null to avoid notifying state change when transitioning
    }
}