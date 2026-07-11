/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase

import android.os.Build

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugEventOccurrenceCounterState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportActionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportCounterInitialValue
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import kotlin.time.Duration.Companion.seconds

/** Tests for [GetDebugOccurrenceAllCounterStateUseCase]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GetDebugOccurrenceAllCounterStateUseCaseTests {

    @Mock private lateinit var mockDebuggingRepository: DebuggingRepository

    private lateinit var useCase: GetDebugOccurrenceAllCounterStateUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    // --- Helpers ---

    /**
     * Stub the repository then create the use case. The use case captures flows at construction
     * time, so mocks must be set up before instantiation.
     */
    private fun mockRepository(
        counterNames: Set<String>,
        initialValues: List<DebugReportCounterInitialValue>?,
        occurrences: List<DebugReportEventOccurrence>,
    ) {
        mockWhen(mockDebuggingRepository.getLastReportOverview()).thenReturn(
            flowOf(
                DebugReportOverview(
                    scenarioId = 1L,
                    duration = 10.seconds,
                    frameCount = 100L,
                    averageFrameProcessingDuration = 0.1.seconds,
                    imageEventFulfilledCount = 0,
                    triggerEventFulfilledCount = 0,
                    counterNames = counterNames,
                )
            )
        )
        mockWhen(mockDebuggingRepository.getLastReportCountersInitialValues()).thenReturn(flowOf(initialValues))
        mockWhen(mockDebuggingRepository.getLastReportEventsOccurrences()).thenReturn(flowOf(occurrences))
        useCase = GetDebugOccurrenceAllCounterStateUseCase(mockDebuggingRepository)
    }

    private fun screenEvent(
        id: Long,
        vararg counterChanges: DebugReportActionResult.CounterChange,
    ): DebugReportEventOccurrence.ScreenEvent =
        DebugReportEventOccurrence.ScreenEvent(
            eventId = id,
            relativeTimestampMs = 0L,
            frameNumber = id,
            conditionsResults = emptyList(),
            counterChanges = counterChanges.toList(),
            eventStateChanges = emptyList(),
        )

    private fun counterChange(name: String, from: Double, to: Double) =
        DebugReportActionResult.CounterChange(counterName = name, previousValue = from, newValue = to)

    private fun counterState(name: String, current: Double, previous: Double? = null) =
        DebugEventOccurrenceCounterState(counterName = name, currentValue = current, previousValue = previous)

    // --- Tests ---

    @Test
    fun noReport_returnsEmpty() = runTest {
        mockWhen(mockDebuggingRepository.getLastReportOverview()).thenReturn(flowOf(null))
        mockWhen(mockDebuggingRepository.getLastReportCountersInitialValues()).thenReturn(flowOf(null))
        mockWhen(mockDebuggingRepository.getLastReportEventsOccurrences()).thenReturn(flowOf(null))
        useCase = GetDebugOccurrenceAllCounterStateUseCase(mockDebuggingRepository)

        val event = screenEvent(id = 1L)
        val result = useCase(event).first()

        assertEquals(emptyList<DebugEventOccurrenceCounterState>(), result)
    }

    @Test
    fun noInitialValues_countersStartAtZero() = runTest {
        val event = screenEvent(id = 1L)
        mockRepository(
            counterNames = setOf("score"),
            initialValues = null,
            occurrences = listOf(event),
        )

        val result = useCase(event).first()

        assertEquals(listOf(counterState("score", 0.0)), result)
    }

    @Test
    fun initialValues_usedAsStartingState() = runTest {
        val event = screenEvent(id = 1L)
        mockRepository(
            counterNames = setOf("score", "lives"),
            initialValues = listOf(
                DebugReportCounterInitialValue("score", 100.0),
                DebugReportCounterInitialValue("lives", 3.0),
            ),
            occurrences = listOf(event),
        )

        val result = useCase(event).first()

        assertEquals(
            listOf(
                counterState("lives", 3.0),
                counterState("score", 100.0),
            ),
            result,
        )
    }

    @Test
    fun initialValues_counterChangedBefore_stateReflectsChange() = runTest {
        val eventBefore = screenEvent(id = 1L, counterChange("score", 100.0, 110.0))
        val targetEvent = screenEvent(id = 2L)
        mockRepository(
            counterNames = setOf("score"),
            initialValues = listOf(DebugReportCounterInitialValue("score", 100.0)),
            occurrences = listOf(eventBefore, targetEvent),
        )

        val result = useCase(targetEvent).first()

        assertEquals(listOf(counterState("score", 110.0)), result)
    }

    @Test
    fun initialValues_counterChangedAtTarget_previousValueShown() = runTest {
        val targetEvent = screenEvent(id = 1L, counterChange("score", 100.0, 150.0))
        mockRepository(
            counterNames = setOf("score"),
            initialValues = listOf(DebugReportCounterInitialValue("score", 100.0)),
            occurrences = listOf(targetEvent),
        )

        val result = useCase(targetEvent).first()

        assertEquals(listOf(counterState("score", current = 150.0, previous = 100.0)), result)
    }

    @Test
    fun initialValues_multipleCounters_orderedByName() = runTest {
        val event = screenEvent(id = 1L)
        mockRepository(
            counterNames = setOf("zeta", "alpha", "mu"),
            initialValues = listOf(
                DebugReportCounterInitialValue("zeta", 1.0),
                DebugReportCounterInitialValue("alpha", 2.0),
                DebugReportCounterInitialValue("mu", 3.0),
            ),
            occurrences = listOf(event),
        )

        val result = useCase(event).first()

        assertEquals(
            listOf(
                counterState("alpha", 2.0),
                counterState("mu", 3.0),
                counterState("zeta", 1.0),
            ),
            result,
        )
    }

    @Test
    fun initialValues_counterNotInInitialValues_fallsBackToZero() = runTest {
        val event = screenEvent(id = 1L)
        mockRepository(
            counterNames = setOf("known", "unknown"),
            initialValues = listOf(DebugReportCounterInitialValue("known", 42.0)),
            occurrences = listOf(event),
        )

        val result = useCase(event).first()

        assertEquals(
            listOf(
                counterState("known", 42.0),
                counterState("unknown", 0.0),
            ),
            result,
        )
    }

    @Test
    fun initialValues_multipleChangesOnSameCounterAtTarget_earliestPreviousValueKept() = runTest {
        val targetEvent = screenEvent(
            id = 1L,
            counterChange("score", 100.0, 110.0),
            counterChange("score", 110.0, 120.0),
        )
        mockRepository(
            counterNames = setOf("score"),
            initialValues = listOf(DebugReportCounterInitialValue("score", 100.0)),
            occurrences = listOf(targetEvent),
        )

        val result = useCase(targetEvent).first()

        assertEquals(listOf(counterState("score", current = 120.0, previous = 100.0)), result)
    }
}
