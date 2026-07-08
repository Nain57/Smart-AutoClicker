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
package com.buzbuz.smartautoclicker.feature.tutorial.domain

import android.os.Build

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategory
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiItems
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GetTutorialCategoryUseCaseTest {

    @Mock private lateinit var mockTutorialRepository: TutorialRepository

    private val completionStateFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private lateinit var mockCloseable: AutoCloseable
    private lateinit var useCase: GetTutorialCategoryUseCase

    @Before
    fun setUp() {
        mockCloseable = MockitoAnnotations.openMocks(this)
        whenever(mockTutorialRepository.tutorialsCompletionState).thenReturn(completionStateFlow)

        useCase = GetTutorialCategoryUseCase(mockTutorialRepository)
    }

    @Test
    fun `invoke emits Loading first`() = runTest(UnconfinedTestDispatcher()) {
        val first = useCase.invoke().first()

        assertEquals(TutorialCategoryUiState.Loading, first)
    }

    @Test
    fun `invoke with default argument loads ROOT category`() = runTest(UnconfinedTestDispatcher()) {
        val state = useCase.invoke().take(2).toList()[1] as TutorialCategoryUiState.Loaded

        assertEquals(R.string.tutorial_category_root_name, state.categoryNameRes)
    }

    @Test
    fun `invoke with ROOT category produces a Header followed by its content`() = runTest(UnconfinedTestDispatcher()) {
        val state = useCase.invoke(TutorialCategory.Type.ROOT).take(2).toList()[1] as TutorialCategoryUiState.Loaded

        val header = state.items[0] as TutorialCategoryUiItems.Header
        assertEquals(R.string.tutorial_category_root_name, header.categoryNameRes)

        // Header + SectionDivider + BASICS + COMBINE_CONDITIONS + EVENT_STATE + COUNTERS
        assertEquals(6, state.items.size)
        assertTrue(state.items[1] is TutorialCategoryUiItems.SectionDivider)
        assertEquals(
            listOf(
                TutorialCategory.Type.BASICS,
                TutorialCategory.Type.COMBINE_CONDITIONS,
                TutorialCategory.Type.EVENT_STATE,
                TutorialCategory.Type.COUNTERS,
            ),
            state.items.filterIsInstance<TutorialCategoryUiItems.Item.Category>().map { it.type },
        )
    }

    @Test
    fun `invoke with IMAGE_CONDITION category produces tutorial items in order`() = runTest(UnconfinedTestDispatcher()) {
        val state = useCase.invoke(TutorialCategory.Type.IMAGE_CONDITION).take(2).toList()[1] as TutorialCategoryUiState.Loaded

        val tutorialItems = state.items.filterIsInstance<TutorialCategoryUiItems.Item.Tutorial>()
        assertEquals(
            listOf(
                TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET,
                TutorialItem.Type.IMAGE_DETECTION_MOVING_TARGET,
            ),
            tutorialItems.map { it.type },
        )
    }

    @Test
    fun `invoke marks tutorial as not completed when absent from completion state`() = runTest(UnconfinedTestDispatcher()) {
        completionStateFlow.value = emptyMap()

        val state = useCase.invoke(TutorialCategory.Type.IMAGE_CONDITION).take(2).toList()[1] as TutorialCategoryUiState.Loaded
        val tutorialItems = state.items.filterIsInstance<TutorialCategoryUiItems.Item.Tutorial>()

        assertTrue(tutorialItems.all { !it.tutorialCompleted })
    }

    @Test
    fun `invoke marks tutorial as completed when present and true in completion state`() = runTest(UnconfinedTestDispatcher()) {
        completionStateFlow.value = mapOf(
            TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET.toTutorialId() to true,
        )

        val state = useCase.invoke(TutorialCategory.Type.IMAGE_CONDITION).take(2).toList()[1] as TutorialCategoryUiState.Loaded
        val tutorialItems = state.items.filterIsInstance<TutorialCategoryUiItems.Item.Tutorial>()

        val completed = tutorialItems.first { it.type == TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET }
        assertTrue(completed.tutorialCompleted)

        val notCompleted = tutorialItems.filterNot { it.type == TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET }
        assertTrue(notCompleted.all { !it.tutorialCompleted })
    }

    @Test
    fun `invoke does not mark tutorial as completed when value is false in completion state`() = runTest(UnconfinedTestDispatcher()) {
        completionStateFlow.value = mapOf(
            TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET.toTutorialId() to false,
        )

        val state = useCase.invoke(TutorialCategory.Type.IMAGE_CONDITION).take(2).toList()[1] as TutorialCategoryUiState.Loaded
        val tutorialItems = state.items.filterIsInstance<TutorialCategoryUiItems.Item.Tutorial>()

        val item = tutorialItems.first { it.type == TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET }
        assertFalse(item.tutorialCompleted)
    }

    @Test
    fun `invoke re-emits a Loaded state when completion state changes`() = runTest(UnconfinedTestDispatcher()) {
        val collected = mutableListOf<TutorialCategoryUiState>()
        val collectJob = launch {
            useCase.invoke(TutorialCategory.Type.IMAGE_CONDITION).collect { collected.add(it) }
        }

        completionStateFlow.value = mapOf(
            TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET.toTutorialId() to true,
        )

        collectJob.cancel()

        assertTrue(collected.size >= 2)
        val lastLoaded = collected.last { it is TutorialCategoryUiState.Loaded } as TutorialCategoryUiState.Loaded
        val tutorialItems = lastLoaded.items.filterIsInstance<TutorialCategoryUiItems.Item.Tutorial>()
        assertTrue(tutorialItems.first { it.type == TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET }.tutorialCompleted)
    }
}
