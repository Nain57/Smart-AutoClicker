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
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
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

class GetScreenEventsForCopyUseCaseTest {

    private val scenarioStateFlow = MutableStateFlow(EditedElementState<Scenario>(value = null, hasChanged = false, canBeSaved = false))
    private val editedScreenEventsStateFlow = MutableStateFlow(
        EditedListState<ScreenEvent>(value = null, itemValidity = emptyList(), hasChanged = false, canBeSaved = false)
    )
    private val allScreenEventsFlow = MutableStateFlow<List<ScreenEvent>>(emptyList())

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }
    private val mockSmartRepository: IRepository = mockk()

    private lateinit var useCase: GetScreenEventsForCopyUseCase

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        every { mockEditionState.scenarioState } returns scenarioStateFlow
        every { mockEditionState.editedScreenEventsState } returns editedScreenEventsStateFlow
        every { mockSmartRepository.allScreenEvents } returns allScreenEventsFlow
        useCase = GetScreenEventsForCopyUseCase(
            dispatcherIo = UnconfinedTestDispatcher(),
            editionRepository = mockEditionRepository,
            smartRepository = mockSmartRepository,
        )
    }

    @Test
    fun `all empty returns empty buckets`() = runTest {
        mockScenarioId(SCENARIO_ID)
        mockEditedEvents(emptyList())
        mockAllDbEvents(emptyList())

        val result = useCase().first()

        assertTrue(result.thisScenario.isEmpty())
        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `complete edited events appear in thisScenario`() = runTest {
        val event = screenEvent(scenarioId = SCENARIO_ID, complete = true)
        mockScenarioId(SCENARIO_ID)
        mockEditedEvents(listOf(event))
        mockAllDbEvents(emptyList())

        val result = useCase().first()

        assertEquals(listOf(event), result.thisScenario)
        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `incomplete edited events are excluded from thisScenario`() = runTest {
        val event = screenEvent(scenarioId = SCENARIO_ID, complete = false)
        mockScenarioId(SCENARIO_ID)
        mockEditedEvents(listOf(event))
        mockAllDbEvents(emptyList())

        val result = useCase().first()

        assertTrue(result.thisScenario.isEmpty())
    }

    @Test
    fun `complete db events from other scenario appear in otherScenario`() = runTest {
        val event = screenEvent(scenarioId = OTHER_SCENARIO_ID, complete = true)
        mockScenarioId(SCENARIO_ID)
        mockEditedEvents(emptyList())
        mockAllDbEvents(listOf(event))

        val result = useCase().first()

        assertTrue(result.thisScenario.isEmpty())
        assertEquals(listOf(event), result.otherScenario)
    }

    @Test
    fun `complete db events from same scenario are excluded from otherScenario`() = runTest {
        val event = screenEvent(scenarioId = SCENARIO_ID, complete = true)
        mockScenarioId(SCENARIO_ID)
        mockEditedEvents(emptyList())
        mockAllDbEvents(listOf(event))

        val result = useCase().first()

        assertTrue(result.otherScenario.isEmpty())
    }

    @Test
    fun `incomplete db events are excluded from otherScenario`() = runTest {
        val event = screenEvent(scenarioId = OTHER_SCENARIO_ID, complete = false)
        mockScenarioId(SCENARIO_ID)
        mockEditedEvents(emptyList())
        mockAllDbEvents(listOf(event))

        val result = useCase().first()

        assertTrue(result.otherScenario.isEmpty())
    }

    // region helpers

    private fun mockScenarioId(id: Identifier) {
        val scenario = mockk<Scenario>(relaxed = true) { every { this@mockk.id } returns id }
        scenarioStateFlow.value = EditedElementState(value = scenario, hasChanged = false, canBeSaved = true)
    }

    private fun mockEditedEvents(events: List<ScreenEvent>) {
        editedScreenEventsStateFlow.value = EditedListState(
            value = events,
            itemValidity = List(events.size) { true },
            hasChanged = false,
            canBeSaved = true,
        )
    }

    private fun mockAllDbEvents(events: List<ScreenEvent>) {
        allScreenEventsFlow.value = events
    }

    private fun screenEvent(scenarioId: Identifier, complete: Boolean): ScreenEvent =
        mockk(relaxed = true) {
            every { this@mockk.scenarioId } returns scenarioId
            every { isComplete() } returns complete
        }

    // endregion

    private companion object {
        val SCENARIO_ID = Identifier(databaseId = 1L)
        val OTHER_SCENARIO_ID = Identifier(databaseId = 2L)
    }
}
