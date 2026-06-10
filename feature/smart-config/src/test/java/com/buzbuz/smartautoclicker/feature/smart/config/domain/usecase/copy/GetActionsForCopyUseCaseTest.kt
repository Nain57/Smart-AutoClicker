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
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedElementState
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

class GetActionsForCopyUseCaseTest {

    private val allEditedEventsFlow = MutableStateFlow<List<Event>>(emptyList())
    private val editedEventStateFlow = MutableStateFlow(EditedElementState<Event>(value = null, hasChanged = false, canBeSaved = false))
    private val allActionsFlow = MutableStateFlow<List<Action>>(emptyList())

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }
    private val mockSmartRepository: IRepository = mockk()

    private lateinit var useCase: GetActionsForCopyUseCase

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        every { mockEditionState.allEditedEventsFlow } returns allEditedEventsFlow
        every { mockEditionState.editedEventState } returns editedEventStateFlow
        every { mockSmartRepository.allActions } returns allActionsFlow
        useCase = GetActionsForCopyUseCase(
            dispatcherIo = UnconfinedTestDispatcher(),
            editionRepository = mockEditionRepository,
            smartRepository = mockSmartRepository,
        )
    }

    @Test
    fun `all empty returns empty ActionsForCopy`() = runTest {
        mockEditedEventState(eventId = EVENT_ID, actions = emptyList())
        mockAllEditedEvents(emptyList())
        mockAllDbActions(emptyList())

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
        assertTrue(result.thisScenario.isEmpty())
        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `complete action in current event appears in thisEvent only`() = runTest {
        val action = action(eventId = EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, actions = listOf(action))
        mockAllEditedEvents(listOf(eventWithActions(EVENT_ID, listOf(action))))
        mockAllDbActions(emptyList())

        val result = useCase().first()

        assertEquals(listOf(action), result.thisEvent)
        assertTrue(result.thisScenario.isEmpty())
    }

    @Test
    fun `incomplete action in current event is excluded`() = runTest {
        val action = action(eventId = EVENT_ID, complete = false)
        mockEditedEventState(eventId = EVENT_ID, actions = listOf(action))
        mockAllEditedEvents(listOf(eventWithActions(EVENT_ID, listOf(action))))
        mockAllDbActions(emptyList())

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
    }

    @Test
    fun `complete action from different event in same scenario appears in thisScenario`() = runTest {
        val actionOtherEvent = action(eventId = OTHER_EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, actions = emptyList())
        mockAllEditedEvents(listOf(
            eventWithActions(EVENT_ID, emptyList()),
            eventWithActions(OTHER_EVENT_ID, listOf(actionOtherEvent)),
        ))
        mockAllDbActions(emptyList())

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
        assertEquals(listOf(actionOtherEvent), result.thisScenario)
        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `complete db action from other scenario appears in otherScenario`() = runTest {
        val dbAction = action(eventId = OTHER_SCENARIO_EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, actions = emptyList())
        mockAllEditedEvents(listOf(eventWithActions(EVENT_ID, emptyList())))
        mockAllDbActions(listOf(dbAction))

        val result = useCase().first()

        assertTrue(result.thisEvent.isEmpty())
        assertTrue(result.thisScenario.isEmpty())
        assertEquals(listOf(dbAction), result.otherScenario)
    }

    @Test
    fun `complete db action from edited scenario is excluded from otherScenario`() = runTest {
        val dbAction = action(eventId = EVENT_ID, complete = true)
        mockEditedEventState(eventId = EVENT_ID, actions = emptyList())
        mockAllEditedEvents(listOf(eventWithActions(EVENT_ID, emptyList())))
        mockAllDbActions(listOf(dbAction))

        val result = useCase().first()

        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `incomplete db action is excluded from otherScenario`() = runTest {
        val dbAction = action(eventId = OTHER_SCENARIO_EVENT_ID, complete = false)
        mockEditedEventState(eventId = EVENT_ID, actions = emptyList())
        mockAllEditedEvents(listOf(eventWithActions(EVENT_ID, emptyList())))
        mockAllDbActions(listOf(dbAction))

        val result = useCase().first()

        assertTrue(result.otherScenario.isEmpty())
    }

    // region helpers

    private fun mockEditedEventState(eventId: Identifier, actions: List<Action>) {
        editedEventStateFlow.value = EditedElementState(
            value = eventWithActions(eventId, actions),
            hasChanged = false,
            canBeSaved = true,
        )
    }

    private fun mockAllEditedEvents(events: List<Event>) {
        allEditedEventsFlow.value = events
    }

    private fun mockAllDbActions(actions: List<Action>) {
        allActionsFlow.value = actions
    }

    private fun eventWithActions(eventId: Identifier, actions: List<Action>): Event =
        mockk(relaxed = true) {
            every { id } returns eventId
            every { this@mockk.actions } returns actions
        }

    private fun action(eventId: Identifier, complete: Boolean): Action =
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
