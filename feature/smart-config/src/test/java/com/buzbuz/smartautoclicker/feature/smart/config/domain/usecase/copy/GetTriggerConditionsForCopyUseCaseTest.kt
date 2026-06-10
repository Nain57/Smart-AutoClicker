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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedElementState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTriggerConditionsForCopyUseCaseTest {

    private val allEditedEventsFlow = MutableStateFlow<List<ScreenEvent>>(emptyList())
    private val editedEventStateFlow = MutableStateFlow(EditedElementState<Event>(value = null, hasChanged = false, canBeSaved = false))
    private val editedTriggerEventsStateFlow = MutableStateFlow(
        EditedListState<TriggerEvent>(value = null, itemValidity = emptyList(), hasChanged = false, canBeSaved = false)
    )
    private val allConditionsFlow = MutableStateFlow<List<Condition>>(emptyList())

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }
    private val mockSmartRepository: IRepository = mockk()

    private lateinit var useCase: GetTriggerConditionsForCopyUseCase

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        every { mockEditionState.allEditedEventsFlow } returns allEditedEventsFlow
        every { mockEditionState.editedEventState } returns editedEventStateFlow
        every { mockEditionState.editedTriggerEventsState } returns editedTriggerEventsStateFlow
        every { mockSmartRepository.allConditions } returns allConditionsFlow
        useCase = GetTriggerConditionsForCopyUseCase(
            dispatcherIo = UnconfinedTestDispatcher(),
            editionRepository = mockEditionRepository,
            smartRepository = mockSmartRepository,
        )
    }

    @Test
    fun `all empty returns empty ConditionsForCopy`() = runTest {
        mockEditedEventState(eventId = EVENT_ID, conditions = emptyList())
        mockAllEditedEvents(emptyList())
        mockEditedTriggerEvents(emptyList())
        mockAllDbConditions(emptyList())

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
        assertTrue(result.thisScenario.isEmpty())
        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `complete condition in current trigger event appears in thisEvent`() = runTest {
        val condition = triggerCondition(eventId = EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, conditions = listOf(condition))
        mockAllEditedEvents(listOf(EVENT_ID))
        mockEditedTriggerEvents(listOf(triggerEventWithConditions(EVENT_ID, listOf(condition))))
        mockAllDbConditions(emptyList())

        val result = useCase().first()

        assertEquals(listOf(condition), result.thisEvent)
        assertTrue(result.thisScenario.isEmpty())
    }

    @Test
    fun `incomplete condition in current trigger event is excluded`() = runTest {
        val condition = triggerCondition(eventId = EVENT_ID, complete = false)
        mockEditedEventState(eventId = EVENT_ID, conditions = listOf(condition))
        mockAllEditedEvents(listOf(EVENT_ID))
        mockEditedTriggerEvents(emptyList())
        mockAllDbConditions(emptyList())

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
    }

    @Test
    fun `complete condition from other trigger event in same scenario appears in thisScenario`() = runTest {
        val condition = triggerCondition(eventId = OTHER_EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, conditions = emptyList())
        mockAllEditedEvents(listOf(EVENT_ID, OTHER_EVENT_ID))
        mockEditedTriggerEvents(listOf(
            triggerEventWithConditions(EVENT_ID, emptyList()),
            triggerEventWithConditions(OTHER_EVENT_ID, listOf(condition)),
        ))
        mockAllDbConditions(emptyList())

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
        assertEquals(listOf(condition), result.thisScenario)
        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `complete db trigger condition from other scenario appears in otherScenario`() = runTest {
        val condition = triggerCondition(eventId = OTHER_SCENARIO_EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, conditions = emptyList())
        mockAllEditedEvents(listOf(EVENT_ID))
        mockEditedTriggerEvents(emptyList())
        mockAllDbConditions(listOf(condition))

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
        assertTrue(result.thisScenario.isEmpty())
        assertEquals(listOf(condition), result.otherScenario)
    }

    @Test
    fun `complete db trigger condition from edited scenario is excluded`() = runTest {
        val condition = triggerCondition(eventId = EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, conditions = emptyList())
        mockAllEditedEvents(listOf(EVENT_ID))
        mockEditedTriggerEvents(emptyList())
        mockAllDbConditions(listOf(condition))

        val result = useCase().first()

        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `incomplete db trigger condition is excluded`() = runTest {
        val condition = triggerCondition(eventId = OTHER_SCENARIO_EVENT_ID, complete = false)
        mockEditedEventState(eventId = EVENT_ID, conditions = emptyList())
        mockAllEditedEvents(listOf(EVENT_ID))
        mockEditedTriggerEvents(emptyList())
        mockAllDbConditions(listOf(condition))

        val result = useCase().first()

        assertTrue(result.otherScenario.isEmpty())
    }

    // region helpers

    private fun mockEditedEventState(eventId: Identifier, conditions: List<TriggerCondition>) {
        editedEventStateFlow.value = EditedElementState(
            value = triggerEventWithConditions(eventId, conditions),
            hasChanged = false,
            canBeSaved = true,
        )
    }

    private fun mockAllEditedEvents(eventIds: List<Identifier>) {
        allEditedEventsFlow.value = eventIds.map { id ->
            mockk<ScreenEvent>(relaxed = true) { every { this@mockk.id } returns id }
        }
    }

    private fun mockEditedTriggerEvents(events: List<TriggerEvent>) {
        editedTriggerEventsStateFlow.value = EditedListState(
            value = events,
            itemValidity = List(events.size) { true },
            hasChanged = false,
            canBeSaved = true,
        )
    }

    private fun mockAllDbConditions(conditions: List<TriggerCondition>) {
        allConditionsFlow.value = conditions
    }

    private fun triggerEventWithConditions(eventId: Identifier, conditions: List<TriggerCondition>): TriggerEvent =
        mockk(relaxed = true) {
            every { id } returns eventId
            every { this@mockk.conditions } returns conditions
        }

    private fun triggerCondition(eventId: Identifier, complete: Boolean): TriggerCondition =
        mockk(relaxed = true) {
            every { this@mockk.eventId } returns eventId
            every { isComplete() } returns complete
        }

    // endregion

    private companion object {
        val EVENT_ID = Identifier(databaseId = 10L)
        val OTHER_EVENT_ID = Identifier(databaseId = 11L)
        val OTHER_SCENARIO_EVENT_ID = Identifier(databaseId = 99L)
    }
}
