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
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DumbEngine @Inject constructor(
    private val dumbRepository: IDumbRepository,
): Dumpable {

    /** Execute the dumb actions. */
    private var dumbActionExecutor: DumbActionExecutor? = null

    /** Coroutine scope for the dumb scenario processing. */
    private var processingScope: CoroutineScope? = null
    /** Job for the scenario auto stop. */
    private var timeoutJob: Job? = null
    /** Job for the scenario execution. */
    private var executionJob: Job? = null

    private val dumbScenarioDbId: MutableStateFlow<Long?> = MutableStateFlow(null)
    val dumbScenario: Flow<DumbScenario?> =
        dumbScenarioDbId.flatMapLatest { dbId ->
            if (dbId == null) return@flatMapLatest flowOf(null)
            dumbRepository.getDumbScenarioFlow(dbId)
        }

    private val _isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    fun init(androidExecutor: AndroidExecutor, dumbScenario: DumbScenario) {
        dumbActionExecutor = DumbActionExecutor(androidExecutor)
        dumbScenarioDbId.value = dumbScenario.id.databaseId

        processingScope = CoroutineScope(Dispatchers.IO)
    }

    fun startDumbScenario() {
        if (_isRunning.value) return

        processingScope?.launch {
            dumbScenarioDbId.value?.let { dbId ->
                dumbRepository.getDumbScenario(dbId)?.let { scenario ->
                    if (scenario.dumbActions.isEmpty()) return@launch

                    _isRunning.value = true

                    Log.d(TAG, "startDumbScenario ${scenario.id} with ${scenario.dumbActions.size} actions")

                    if (!scenario.isDurationInfinite) timeoutJob = startTimeoutJob(scenario.maxDurationMin)
                    executionJob = startScenarioExecutionJob(scenario)
                }
            }
        }
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

        dumbScenarioDbId.value = null
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

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* DumbEngine:")

            append(contentPrefix)
                .append("- scenarioId=${dumbScenarioDbId.value}; ")
                .append("isRunning=${isRunning.value}; ")
                .println()
        }
    }
}

private const val TAG = "DumbEngine"