/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.switcher

import android.content.Context
import android.content.Intent

import com.buzbuz.smartautoclicker.core.base.ScenarioStats
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortSettings
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortType

import io.mockk.mockk

import java.io.PrintWriter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioSwitchViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state excludes current scenario and follows shared sort settings`() = runTest {
        val currentId = Identifier(databaseId = 1L)
        val scenarios = MutableStateFlow(
            listOf(
                scenario(1L, "Current", lastStart = 100L),
                scenario(2L, "Recent", lastStart = 300L),
                scenario(3L, "Old", lastStart = 200L),
            ),
        )
        val processingRepository = TestProcessingRepository(
            scenarioId = MutableStateFlow(currentId),
            detectionState = MutableStateFlow(DetectionState.RECORDING),
        )
        val settingsRepository = TestSettingsRepository(
            ScenarioSortSettings(
                type = ScenarioSortType.RECENT,
                inverted = false,
                showSmartScenario = false,
                showDumbScenario = false,
            ),
        )

        val viewModel = ScenarioSwitchViewModel(
            smartRepository = TestScenarioRepository(scenarios),
            smartProcessingRepository = processingRepository,
            settingsRepository = settingsRepository,
        )

        val state = viewModel.uiState.first { it.currentScenario != null }

        assertEquals("Current", state.currentScenario?.name)
        assertEquals(listOf(2L, 3L), state.alternatives.map { it.id.databaseId })
        assertEquals(true, state.isPaused)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `missing current scenario produces no switch targets`() = runTest {
        val scenarios = MutableStateFlow(listOf(scenario(2L, "Target", lastStart = 300L)))
        val viewModel = ScenarioSwitchViewModel(
            smartRepository = TestScenarioRepository(scenarios),
            smartProcessingRepository = TestProcessingRepository(
                scenarioId = MutableStateFlow(null),
                detectionState = MutableStateFlow(DetectionState.RECORDING),
            ),
            settingsRepository = TestSettingsRepository(
                ScenarioSortSettings(
                    type = ScenarioSortType.NAME,
                    inverted = false,
                    showSmartScenario = true,
                    showDumbScenario = true,
                ),
            ),
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(null, state.currentScenario)
        assertEquals(emptyList<Scenario>(), state.alternatives)
        assertEquals(false, state.isPaused)
    }

    private fun scenario(id: Long, name: String, lastStart: Long) = Scenario(
        id = Identifier(databaseId = id),
        name = name,
        detectionQuality = 0,
        stats = ScenarioStats(lastStartTimestampMs = lastStart, startCount = 0),
    )
}

private class TestSettingsRepository(settings: ScenarioSortSettings) : SettingsRepository {
    override val isLegacyActionUiEnabledFlow: Flow<Boolean> = flowOf(false)
    override val isLegacyNotificationUiEnabledFlow: Flow<Boolean> = flowOf(false)
    override val isEntireScreenCaptureForcedFlow: Flow<Boolean> = flowOf(false)
    override val isFilterScenarioUiEnabledFlow: Flow<Boolean> = flowOf(false)
    override val isScenarioSwitcherEnabledFlow: Flow<Boolean> = flowOf(false)
    override suspend fun isScenarioSwitcherEnabled(): Boolean = false
    override val isInputBlockWorkaroundEnabledFlow: Flow<Boolean> = flowOf(false)
    override val scenarioSortSettings: Flow<ScenarioSortSettings> = MutableStateFlow(settings)

    override fun isLegacyActionUiEnabled() = false
    override fun toggleLegacyActionUi() = Unit
    override fun isLegacyNotificationUiEnabled() = false
    override fun toggleLegacyNotificationUi() = Unit
    override fun isEntireScreenCaptureForced() = false
    override fun toggleForceEntireScreenCapture() = Unit
    override fun toggleFilterScenarioUi() = Unit
    override fun toggleScenarioSwitcher() = Unit
    override fun isInputBlockWorkaroundEnabled() = false
    override fun toggleInputBlockWorkaround() = Unit
    override fun setScenarioSortType(type: ScenarioSortType) = Unit
    override fun setScenarioSortOrder(invertSortOrder: Boolean) = Unit
    override fun setScenarioSortShowDumb(show: Boolean) = Unit
    override fun setScenarioSortShowSmart(show: Boolean) = Unit
}

private class TestProcessingRepository(
    override val scenarioId: StateFlow<Identifier?>,
    override val detectionState: Flow<DetectionState>,
) : SmartProcessingRepository {
    override val canStartDetection: Flow<Boolean> = emptyFlow()
    override fun getScenarioId() = scenarioId.value
    override fun isRunning() = false
    override fun isScreenRecordActive() = false
    override fun setScenarioId(identifier: Identifier, markAsUsed: Boolean) = Unit
    override suspend fun setScenarioIdAndMarkAsUsed(identifier: Identifier) = Unit
    override fun setProjectionErrorHandler(handler: () -> Unit) = Unit
    override fun startScreenRecord(resultCode: Int, data: Intent) = Unit
    override suspend fun startDetection(context: Context, liveDebugging: Boolean, generateReport: Boolean, autoStopDuration: Duration?) = Unit
    override fun stopDetection() = Unit
    override fun stopScreenRecord() = Unit
    override suspend fun tryEvent(context: Context, scenario: Scenario, event: ScreenEvent) = Unit
    override suspend fun tryScreenCondition(context: Context, scenario: Scenario, condition: ScreenCondition) = Unit
    override suspend fun tryAction(context: Context, scenario: Scenario, action: Action) = Unit
    override fun dump(writer: PrintWriter, prefix: CharSequence) = Unit
}

private class TestScenarioRepository(
    override val scenarios: Flow<List<Scenario>>,
) : IRepository by mockk(relaxed = true) {
    override val allScreenEvents: Flow<List<ScreenEvent>> = emptyFlow()
    override val allTriggerEvents: Flow<List<TriggerEvent>> = emptyFlow()
    override val allConditions: Flow<List<Condition>> = emptyFlow()
    override val allActions: Flow<List<Action>> = emptyFlow()
    override val screenEventsCount: Flow<Int> = emptyFlow()
    override val triggerEventsCount: Flow<Int> = emptyFlow()
    override val screenConditionsCount: Flow<Int> = emptyFlow()
    override val triggerConditionsCount: Flow<Int> = emptyFlow()
    override val actionsCount: Flow<Int> = emptyFlow()
    override val legacyConditionsCount: Flow<Int> = emptyFlow()

    override suspend fun getScenario(scenarioId: Long) = scenarios.first().firstOrNull { it.id.databaseId == scenarioId }
}
