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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.availability

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState
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

class IsScreenConditionCopyAvailableUseCaseTest {

    private val editedScreenEventsStateFlow = MutableStateFlow(
        EditedListState<ScreenEvent>(value = null, itemValidity = emptyList(), hasChanged = false, canBeSaved = false)
    )
    private val screenConditionsCountFlow = MutableStateFlow(0)

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }
    private val mockSmartRepository: IRepository = mockk()

    private lateinit var useCase: IsScreenConditionCopyAvailableUseCase

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        every { mockEditionState.editedScreenEventsState } returns editedScreenEventsStateFlow
        every { mockSmartRepository.screenConditionsCount } returns screenConditionsCountFlow
        useCase = IsScreenConditionCopyAvailableUseCase(
            dispatcherIo = UnconfinedTestDispatcher(),
            editionRepository = mockEditionRepository,
            smartRepository = mockSmartRepository,
        )
    }

    @Test
    fun `no edited conditions and no db conditions returns false`() = runTest {
        mockEditedEvents(listOf(screenEventWithConditions(emptyList())))
        mockConditionsCount(0)

        assertFalse(useCase().first())
    }

    @Test
    fun `event with conditions exists returns true`() = runTest {
        val condition = mockk<ScreenCondition>(relaxed = true)
        mockEditedEvents(listOf(screenEventWithConditions(listOf(condition))))
        mockConditionsCount(0)

        assertTrue(useCase().first())
    }

    @Test
    fun `database conditions exist returns true`() = runTest {
        mockEditedEvents(listOf(screenEventWithConditions(emptyList())))
        mockConditionsCount(4)

        assertTrue(useCase().first())
    }

    @Test
    fun `events exist but have no conditions returns false when db is empty`() = runTest {
        mockEditedEvents(listOf(screenEventWithConditions(emptyList()), screenEventWithConditions(emptyList())))
        mockConditionsCount(0)

        assertFalse(useCase().first())
    }

    @Test
    fun `null edited events list returns false when db is empty`() = runTest {
        editedScreenEventsStateFlow.value = EditedListState(value = null, itemValidity = emptyList(), hasChanged = false, canBeSaved = false)
        mockConditionsCount(0)

        assertFalse(useCase().first())
    }

    // region helpers

    private fun mockEditedEvents(events: List<ScreenEvent>) {
        editedScreenEventsStateFlow.value = EditedListState(
            value = events,
            itemValidity = List(events.size) { true },
            hasChanged = false,
            canBeSaved = true,
        )
    }

    private fun mockConditionsCount(count: Int) {
        screenConditionsCountFlow.value = count
    }

    private fun screenEventWithConditions(conditions: List<ScreenCondition>): ScreenEvent =
        mockk(relaxed = true) { every { this@mockk.conditions } returns conditions }

    // endregion
}
