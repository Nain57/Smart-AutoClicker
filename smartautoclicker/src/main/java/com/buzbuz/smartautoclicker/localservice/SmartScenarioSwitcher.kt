/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.localservice

import android.util.Log

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.processing.domain.model.ScenarioSwitchResult

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

/** Coordinates all scenario changes made while a Smart service session is paused. */
internal class SmartScenarioSwitcher(
    private val smartProcessingRepository: SmartProcessingRepository,
    private val smartRepository: IRepository,
    private val isServiceAvailable: () -> Boolean,
    private val serviceSessionId: () -> Long?,
    private val scenarioTransitionMutex: Mutex,
    private val onScenarioChanged: (Scenario) -> Unit,
) {

    private val switchMutex = Mutex()

    suspend fun switchTo(scenario: Scenario): ScenarioSwitchResult = switchMutex.withLock switcherLock@{
        val switchingSessionId = serviceSessionId()
            ?: return@switcherLock ScenarioSwitchResult.ServiceUnavailable

        scenarioTransitionMutex.withLock transitionLock@{
            if (!isServiceAvailable() || serviceSessionId() != switchingSessionId) {
                return@transitionLock ScenarioSwitchResult.ServiceUnavailable
            }
            validatePausedRecordingState()?.let { return@transitionLock it }

            val currentScenarioId = smartProcessingRepository.getScenarioId()
                ?: return@transitionLock ScenarioSwitchResult.InvalidProcessingState

            if (currentScenarioId == scenario.id) {
                return@transitionLock ScenarioSwitchResult.CurrentScenario
            }

            val storedScenario = try {
                smartRepository.getScenario(scenario.id.databaseId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                return@transitionLock ScenarioSwitchResult.PersistenceFailure
            }
                ?: return@transitionLock ScenarioSwitchResult.ScenarioUnavailable

            if (storedScenario.id == currentScenarioId) {
                return@transitionLock ScenarioSwitchResult.CurrentScenario
            }

            // The database lookup above suspends. Revalidate both the session and projection afterwards so neither a
            // quick service restart nor a projection loss can install a scenario into an unrelated session.
            if (!isServiceAvailable() || serviceSessionId() != switchingSessionId) {
                return@transitionLock ScenarioSwitchResult.ServiceUnavailable
            }
            validatePausedRecordingState()?.let { return@transitionLock it }

            try {
                // This is the only usage-marking call for a switch. It completes before Success is returned and
                // leaves the active screen projection untouched.
                smartProcessingRepository.setScenarioIdAndMarkAsUsed(storedScenario.id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                return@transitionLock ScenarioSwitchResult.PersistenceFailure
            }

            // A service stop can race the final persistence call. The scenario and its usage have already been saved,
            // so report that successful choice accurately, but do not send callbacks into a torn-down service.
            if (isServiceAvailable() && serviceSessionId() == switchingSessionId) {
                try {
                    onScenarioChanged(storedScenario)
                } catch (error: Exception) {
                    // Reporting is best-effort as well: neither a notification failure nor a logging failure may
                    // change the result of an already committed scenario switch.
                    runCatching {
                        Log.w(TAG, "Scenario was switched but post-switch UI synchronization failed", error)
                    }
                }
            }

            ScenarioSwitchResult.Success
        }
    }

    private suspend fun validatePausedRecordingState(): ScenarioSwitchResult? = when (
        smartProcessingRepository.detectionState.first()
    ) {
        DetectionState.RECORDING -> null
        DetectionState.INACTIVE,
        DetectionState.ERROR_SCREEN_IMAGE_CAPTURE_FAILED,
        -> ScenarioSwitchResult.ProjectionUnavailable
        else -> ScenarioSwitchResult.InvalidProcessingState
    }
}

private const val TAG = "SmartScenarioSwitcher"
