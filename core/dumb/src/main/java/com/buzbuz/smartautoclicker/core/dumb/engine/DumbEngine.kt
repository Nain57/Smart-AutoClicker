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
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

internal class DumbEngine {

    /** Execute the dumb actions. */
    private var dumbActionExecutor: DumbActionExecutor? = null

    /** Coroutine scope for the dumb scenario processing. */
    private var processingScope: CoroutineScope? = null
    /** Job for the scenario auto stop. */
    private var timeoutJob: Job? = null
    /** Job for the scenario execution. */
    private var executionJob: Job? = null

    private val _isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    fun init(androidExecutor: AndroidExecutor) {
        dumbActionExecutor = DumbActionExecutor(androidExecutor)
        processingScope = CoroutineScope(Dispatchers.IO)
    }

    fun startDumbScenario(dumbScenario: DumbScenario) {
        if (_isRunning.value) return
        if (dumbScenario.dumbActions.isEmpty()) return
        _isRunning.value = true

        Log.d(TAG, "startDumbScenario ${dumbScenario.id} with ${dumbScenario.dumbActions.size} actions")

        if (!dumbScenario.isDurationInfinite) timeoutJob = startTimeoutJob(dumbScenario.maxDurationMin)
        executionJob = startScenarioExecutionJob(dumbScenario)
    }

    fun stopDumbScenario() {
        if (!isRunning.value) return
        _isRunning.value = false

        Log.d(TAG, "stopDumbScenario")

        timeoutJob?.cancel()
        timeoutJob = null
        executionJob?.cancel()
        executionJob = null
    }

    fun release() {
        if (isRunning.value) stopDumbScenario()

        processingScope?.cancel()
        processingScope = null

        dumbActionExecutor = null
    }

    private fun startTimeoutJob(timeoutDurationMinutes: Int): Job? =
        processingScope?.launch {
            Log.d(TAG, "startTimeoutJob: timeoutDurationMinutes=$timeoutDurationMinutes")
            delay(timeoutDurationMinutes.minutes.inWholeMilliseconds)

            processingScope?.launch { stopDumbScenario() }
        }

    private fun startScenarioExecutionJob(dumbScenario: DumbScenario): Job? =
        processingScope?.launch {
            dumbScenario.repeat {
                dumbScenario.dumbActions.forEach { dumbAction ->
                    dumbActionExecutor?.executeDumbAction(dumbAction, dumbScenario.randomize)
                }
            }

            processingScope?.launch { stopDumbScenario() }
        }
}

private const val TAG = "DumbEngine"