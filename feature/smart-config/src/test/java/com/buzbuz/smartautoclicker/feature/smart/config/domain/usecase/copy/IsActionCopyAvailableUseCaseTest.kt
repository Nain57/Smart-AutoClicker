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

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IsActionCopyAvailableUseCaseTest {

    private val allEditedEventsFlow = MutableStateFlow<List<Event>>(emptyList())
    private val actionsCountFlow = MutableStateFlow(0)

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }
    private val mockSmartRepository: IRepository = mockk()

    private lateinit var useCase: IsActionCopyAvailableUseCase

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        every { mockEditionState.allEditedEventsFlow } returns allEditedEventsFlow
        every { mockSmartRepository.actionsCount } returns actionsCountFlow
        useCase = IsActionCopyAvailableUseCase(
            dispatcherIo = UnconfinedTestDispatcher(),
            editionRepository = mockEditionRepository,
            smartRepository = mockSmartRepository,
        )
    }

    @Test
    fun `no edited actions and no db actions returns false`() = runTest {
        mockEditedEvents(listOf(eventWithActions(emptyList())))
        mockActionsCount(0)

        assertFalse(useCase().first())
    }

    @Test
    fun `edited event with actions returns true`() = runTest {
        val action = mockk<Action>(relaxed = true)
        mockEditedEvents(listOf(eventWithActions(listOf(action))))
        mockActionsCount(0)

        assertTrue(useCase().first())
    }

    @Test
    fun `database actions exist returns true`() = runTest {
        mockEditedEvents(listOf(eventWithActions(emptyList())))
        mockActionsCount(5)

        assertTrue(useCase().first())
    }

    @Test
    fun `both edited and database actions exist returns true`() = runTest {
        val action = mockk<Action>(relaxed = true)
        mockEditedEvents(listOf(eventWithActions(listOf(action))))
        mockActionsCount(3)

        assertTrue(useCase().first())
    }

    @Test
    fun `no edited events returns false when db is empty`() = runTest {
        mockEditedEvents(emptyList())
        mockActionsCount(0)

        assertFalse(useCase().first())
    }

    // region helpers

    private fun mockEditedEvents(events: List<Event>) {
        allEditedEventsFlow.value = events
    }

    private fun mockActionsCount(count: Int) {
        actionsCountFlow.value = count
    }

    private fun eventWithActions(actions: List<Action>): Event =
        mockk(relaxed = true) { every { this@mockk.actions } returns actions }

    // endregion
}
