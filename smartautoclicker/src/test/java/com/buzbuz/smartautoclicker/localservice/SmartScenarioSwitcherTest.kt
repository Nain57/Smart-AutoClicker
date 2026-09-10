/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.localservice

import android.content.Context
import android.content.Intent

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
import com.buzbuz.smartautoclicker.core.processing.domain.model.ScenarioSwitchResult

import io.mockk.mockk

import java.io.PrintWriter

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class SmartScenarioSwitcherTest {

    private val currentScenarioId = Identifier(databaseId = 1L)
    private val targetScenario = scenario(2L, "Target")
    private lateinit var processingRepository: TestProcessingRepository
    private lateinit var scenarioRepository: TestScenarioRepository
    private lateinit var switcher: SmartScenarioSwitcher
    private var serviceAvailable = true
    private var serviceSessionId = 1L
    private val changedScenarios = mutableListOf<Scenario>()
    private var onScenarioChanged: (Scenario) -> Unit = changedScenarios::add

    @Before
    fun setUp() {
        serviceAvailable = true
        serviceSessionId = 1L
        changedScenarios.clear()
        onScenarioChanged = changedScenarios::add
        processingRepository = TestProcessingRepository(currentScenarioId)
        scenarioRepository = TestScenarioRepository(listOf(targetScenario))

        switcher = SmartScenarioSwitcher(
            smartProcessingRepository = processingRepository,
            smartRepository = scenarioRepository,
            isServiceAvailable = { serviceAvailable },
            serviceSessionId = { serviceSessionId.takeIf { serviceAvailable } },
            scenarioTransitionMutex = Mutex(),
            onScenarioChanged = { onScenarioChanged(it) },
        )
    }

    @Test
    fun `paused switch marks target used exactly once and notifies once`() = runTest {
        assertEquals(ScenarioSwitchResult.Success, switcher.switchTo(targetScenario))

        assertEquals(listOf(targetScenario.id), processingRepository.markedAsUsed)
        assertEquals(listOf(targetScenario), changedScenarios)
        assertEquals(0, processingRepository.stopScreenRecordCalls)
        assertEquals(0, processingRepository.startScreenRecordCalls)
    }

    @Test
    fun `duplicate switch requests are serialized and mark target once`() = runTest {
        val setStarted = CompletableDeferred<Unit>()
        val releaseSet = CompletableDeferred<Unit>()
        processingRepository.onMarkAsUsed = {
            setStarted.complete(Unit)
            releaseSet.await()
        }

        val firstRequest = async { switcher.switchTo(targetScenario) }
        setStarted.await()
        val secondRequest = async { switcher.switchTo(targetScenario) }
        runCurrent()

        assertFalse(secondRequest.isCompleted)
        releaseSet.complete(Unit)
        val results = listOf(firstRequest.await(), secondRequest.await())

        assertEquals(1, results.count { it == ScenarioSwitchResult.Success })
        assertEquals(1, results.count { it == ScenarioSwitchResult.CurrentScenario })
        assertEquals(listOf(targetScenario.id), processingRepository.markedAsUsed)
    }

    @Test
    fun `current stale and invalid targets never mark usage`() = runTest {
        assertEquals(ScenarioSwitchResult.CurrentScenario, switcher.switchTo(scenario(1L, "Current")))

        assertEquals(ScenarioSwitchResult.ScenarioUnavailable, switcher.switchTo(scenario(3L, "Deleted")))

        processingRepository.detectionStateValue.value = DetectionState.DETECTING
        assertEquals(ScenarioSwitchResult.InvalidProcessingState, switcher.switchTo(targetScenario))

        assertEquals(emptyList<Identifier>(), processingRepository.markedAsUsed)
    }

    @Test
    fun `projection loss is reported without changing the loaded scenario`() = runTest {
        processingRepository.detectionStateValue.value = DetectionState.INACTIVE

        assertEquals(ScenarioSwitchResult.ProjectionUnavailable, switcher.switchTo(targetScenario))
        assertEquals(emptyList<Identifier>(), processingRepository.markedAsUsed)
    }

    @Test
    fun `projection loss during scenario lookup prevents a late switch`() = runTest {
        scenarioRepository.onGetScenario = {
            processingRepository.detectionStateValue.value = DetectionState.INACTIVE
            targetScenario
        }

        assertEquals(ScenarioSwitchResult.ProjectionUnavailable, switcher.switchTo(targetScenario))
        assertEquals(emptyList<Identifier>(), processingRepository.markedAsUsed)
        assertEquals(currentScenarioId, processingRepository.getScenarioId())
    }

    @Test
    fun `usage persistence failure does not change the loaded scenario`() = runTest {
        processingRepository.onMarkAsUsed = { throw IllegalStateException("database") }

        assertEquals(ScenarioSwitchResult.PersistenceFailure, switcher.switchTo(targetScenario))
        assertEquals(currentScenarioId, processingRepository.getScenarioId())
        assertEquals(emptyList<Scenario>(), changedScenarios)
    }

    @Test
    fun `service stop during persistence keeps the successful switch but skips the callback`() = runTest {
        processingRepository.onMarkAsUsed = { serviceAvailable = false }

        assertEquals(ScenarioSwitchResult.Success, switcher.switchTo(targetScenario))
        assertEquals(targetScenario.id, processingRepository.getScenarioId())
        assertEquals(emptyList<Scenario>(), changedScenarios)
    }

    @Test
    fun `service restart during scenario lookup prevents a stale switch`() = runTest {
        scenarioRepository.onGetScenario = {
            serviceSessionId++
            targetScenario
        }

        assertEquals(ScenarioSwitchResult.ServiceUnavailable, switcher.switchTo(targetScenario))
        assertEquals(emptyList<Identifier>(), processingRepository.markedAsUsed)
        assertEquals(currentScenarioId, processingRepository.getScenarioId())
    }

    @Test
    fun `post switch callback failure does not turn a completed switch into a failure`() = runTest {
        onScenarioChanged = { throw IllegalStateException("notification") }

        assertEquals(ScenarioSwitchResult.Success, switcher.switchTo(targetScenario))
        assertEquals(targetScenario.id, processingRepository.getScenarioId())
    }

    private fun scenario(id: Long, name: String) = Scenario(
        id = Identifier(databaseId = id),
        name = name,
        detectionQuality = 0,
    )
}

private class TestProcessingRepository(initialScenarioId: Identifier) : SmartProcessingRepository {
    private val scenarioIdValue = MutableStateFlow<Identifier?>(initialScenarioId)
    val detectionStateValue = MutableStateFlow(DetectionState.RECORDING)
    val markedAsUsed = mutableListOf<Identifier>()
    var onMarkAsUsed: suspend () -> Unit = {}
    var startScreenRecordCalls = 0
    var stopScreenRecordCalls = 0

    override val scenarioId: StateFlow<Identifier?> = scenarioIdValue
    override val canStartDetection: Flow<Boolean> = emptyFlow()
    override val detectionState: Flow<DetectionState> = detectionStateValue

    override fun getScenarioId() = scenarioIdValue.value
    override fun isRunning() = detectionStateValue.value == DetectionState.DETECTING
    override fun setScenarioId(identifier: Identifier, markAsUsed: Boolean) { scenarioIdValue.value = identifier }
    override suspend fun setScenarioIdAndMarkAsUsed(identifier: Identifier) {
        onMarkAsUsed()
        markedAsUsed += identifier
        scenarioIdValue.value = identifier
    }
    override fun setProjectionErrorHandler(handler: () -> Unit) = Unit
    override fun startScreenRecord(resultCode: Int, data: Intent) { startScreenRecordCalls++ }
    override suspend fun startDetection(context: Context, liveDebugging: Boolean, generateReport: Boolean, autoStopDuration: Duration?) = Unit
    override fun stopDetection() = Unit
    override fun stopScreenRecord() { stopScreenRecordCalls++ }
    override suspend fun tryEvent(context: Context, scenario: Scenario, event: ScreenEvent) = Unit
    override suspend fun tryScreenCondition(context: Context, scenario: Scenario, condition: ScreenCondition) = Unit
    override suspend fun tryAction(context: Context, scenario: Scenario, action: Action) = Unit
    override fun dump(writer: PrintWriter, prefix: CharSequence) = Unit
}

private class TestScenarioRepository(initialScenarios: List<Scenario>) : IRepository by mockk(relaxed = true) {
    private val scenariosValue = MutableStateFlow(initialScenarios)
    var onGetScenario: suspend (Long) -> Scenario? = { scenarioId ->
        scenariosValue.value.firstOrNull { it.id.databaseId == scenarioId }
    }

    override val scenarios: Flow<List<Scenario>> = scenariosValue
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

    override suspend fun getScenario(scenarioId: Long) = onGetScenario(scenarioId)
}
