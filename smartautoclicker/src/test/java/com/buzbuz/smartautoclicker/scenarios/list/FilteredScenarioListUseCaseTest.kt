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
package com.buzbuz.smartautoclicker.scenarios.list

import android.content.Context
import com.buzbuz.smartautoclicker.core.base.ScenarioStats
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortSettings
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortType
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilteredScenarioListUseCaseTest {

    private val dumbScenariosFlow = MutableStateFlow<List<DumbScenario>>(emptyList())
    private val smartScenariosFlow = MutableStateFlow<List<Scenario>>(emptyList())
    private val sortSettingsFlow = MutableStateFlow(DEFAULT_SORT_SETTINGS)
    private val filterUiEnabledFlow = MutableStateFlow(false)
    private val searchQueryFlow = MutableStateFlow<String?>(null)

    private val mockContext: Context = mockk(relaxed = true)
    private val mockDumbRepository: IDumbRepository = mockk()
    private val mockSmartRepository: IRepository = mockk()
    private val mockSettingsRepository: SettingsRepository = mockk()

    private lateinit var useCase: FilteredScenarioListUseCase

    @Before
    fun setUp() {
        every { mockDumbRepository.dumbScenarios } returns dumbScenariosFlow
        every { mockSmartRepository.scenarios } returns smartScenariosFlow
        every { mockSettingsRepository.scenarioSortSettings } returns sortSettingsFlow
        every { mockSettingsRepository.isFilterScenarioUiEnabledFlow } returns filterUiEnabledFlow
        useCase = FilteredScenarioListUseCase(
            context = mockContext,
            dumbRepository = mockDumbRepository,
            settingsRepository = mockSettingsRepository,
            smartRepository = mockSmartRepository,
        )
    }

    // region filter ui disabled

    @Test
    fun `no scenarios, filter disabled, no search returns empty list`() = runTest {
        filterUiEnabledFlow.value = false
        dumbScenariosFlow.value = emptyList()
        smartScenariosFlow.value = emptyList()

        val result = useCase(searchQueryFlow).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter disabled, no search returns all scenarios without SortItem`() = runTest {
        filterUiEnabledFlow.value = false
        dumbScenariosFlow.value = listOf(dumbScenario(id = 1, name = "Bravo"))
        smartScenariosFlow.value = listOf(smartScenario(id = 2, name = "Alpha"))

        val result = useCase(searchQueryFlow).first()

        assertEquals(2, result.size)
        assertTrue(result.none { it is ScenarioListUiState.Item.SortItem })
    }

    // endregion

    // region filter ui enabled – SortItem presence

    @Test
    fun `filter enabled, non-empty list has SortItem as first item`() = runTest {
        filterUiEnabledFlow.value = true
        smartScenariosFlow.value = listOf(smartScenario(id = 1, name = "Alpha"))

        val result = useCase(searchQueryFlow).first()

        assertTrue(result.first() is ScenarioListUiState.Item.SortItem)
    }

    @Test
    fun `filter enabled, empty list has no SortItem`() = runTest {
        filterUiEnabledFlow.value = true
        dumbScenariosFlow.value = emptyList()
        smartScenariosFlow.value = emptyList()

        val result = useCase(searchQueryFlow).first()

        assertTrue(result.none { it is ScenarioListUiState.Item.SortItem })
    }

    @Test
    fun `SortItem reflects current sort settings`() = runTest {
        filterUiEnabledFlow.value = true
        smartScenariosFlow.value = listOf(smartScenario(id = 1, name = "Alpha"))
        sortSettingsFlow.value = ScenarioSortSettings(
            type = ScenarioSortType.RECENT,
            inverted = true,
            showSmartScenario = true,
            showDumbScenario = false,
        )

        val sortItem = useCase(searchQueryFlow).first()
            .filterIsInstance<ScenarioListUiState.Item.SortItem>().first()

        assertEquals(ScenarioSortType.RECENT, sortItem.sortType)
        assertEquals(true, sortItem.changeOrderChecked)
        assertEquals(true, sortItem.smartVisible)
        assertEquals(false, sortItem.dumbVisible)
    }

    // endregion

    // region visibility filter

    @Test
    fun `filter enabled, showSmart false hides smart scenarios`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(showSmartScenario = false, showDumbScenario = true)
        dumbScenariosFlow.value = listOf(dumbScenario(id = 1, name = "Dumb"))
        smartScenariosFlow.value = listOf(smartScenario(id = 2, name = "Smart"))

        val scenarios = scenarioItems(useCase(searchQueryFlow).first())

        assertTrue(scenarios.all { it.scenario is DumbScenario })
    }

    @Test
    fun `filter enabled, showDumb false hides dumb scenarios`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(showSmartScenario = true, showDumbScenario = false)
        dumbScenariosFlow.value = listOf(dumbScenario(id = 1, name = "Dumb"))
        smartScenariosFlow.value = listOf(smartScenario(id = 2, name = "Smart"))

        val scenarios = scenarioItems(useCase(searchQueryFlow).first())

        assertTrue(scenarios.all { it.scenario is Scenario })
    }

    @Test
    fun `filter enabled, both false returns no scenario items`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(showSmartScenario = false, showDumbScenario = false)
        dumbScenariosFlow.value = listOf(dumbScenario(id = 1, name = "Dumb"))
        smartScenariosFlow.value = listOf(smartScenario(id = 2, name = "Smart"))

        val scenarios = scenarioItems(useCase(searchQueryFlow).first())

        assertTrue(scenarios.isEmpty())
    }

    // endregion

    // region sort by NAME

    @Test
    fun `sort by NAME ascending orders alphabetically`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(type = ScenarioSortType.NAME, inverted = false)
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Charlie"),
            smartScenario(id = 2, name = "Alpha"),
            smartScenario(id = 3, name = "Bravo"),
        )

        val names = scenarioItems(useCase(searchQueryFlow).first()).map { it.displayName }

        assertEquals(listOf("Alpha", "Bravo", "Charlie"), names)
    }

    @Test
    fun `sort by NAME descending orders reverse alphabetically`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(type = ScenarioSortType.NAME, inverted = true)
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Charlie"),
            smartScenario(id = 2, name = "Alpha"),
            smartScenario(id = 3, name = "Bravo"),
        )

        val names = scenarioItems(useCase(searchQueryFlow).first()).map { it.displayName }

        assertEquals(listOf("Charlie", "Bravo", "Alpha"), names)
    }

    // endregion

    // region sort by RECENT

    @Test
    fun `sort by RECENT non-inverted orders most recent first`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(type = ScenarioSortType.RECENT, inverted = false)
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Old", lastStart = 100L),
            smartScenario(id = 2, name = "New", lastStart = 300L),
            smartScenario(id = 3, name = "Mid", lastStart = 200L),
        )

        val names = scenarioItems(useCase(searchQueryFlow).first()).map { it.displayName }

        assertEquals(listOf("New", "Mid", "Old"), names)
    }

    @Test
    fun `sort by RECENT inverted orders oldest first`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(type = ScenarioSortType.RECENT, inverted = true)
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Old", lastStart = 100L),
            smartScenario(id = 2, name = "New", lastStart = 300L),
            smartScenario(id = 3, name = "Mid", lastStart = 200L),
        )

        val names = scenarioItems(useCase(searchQueryFlow).first()).map { it.displayName }

        assertEquals(listOf("Old", "Mid", "New"), names)
    }

    // endregion

    // region sort by MOST_USED

    @Test
    fun `sort by MOST_USED non-inverted orders most used first`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(type = ScenarioSortType.MOST_USED, inverted = false)
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Rare", startCount = 1L),
            smartScenario(id = 2, name = "Popular", startCount = 50L),
            smartScenario(id = 3, name = "Normal", startCount = 10L),
        )

        val names = scenarioItems(useCase(searchQueryFlow).first()).map { it.displayName }

        assertEquals(listOf("Popular", "Normal", "Rare"), names)
    }

    @Test
    fun `sort by MOST_USED inverted orders least used first`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(type = ScenarioSortType.MOST_USED, inverted = true)
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Rare", startCount = 1L),
            smartScenario(id = 2, name = "Popular", startCount = 50L),
            smartScenario(id = 3, name = "Normal", startCount = 10L),
        )

        val names = scenarioItems(useCase(searchQueryFlow).first()).map { it.displayName }

        assertEquals(listOf("Rare", "Normal", "Popular"), names)
    }

    // endregion

    // region sort type change resets inverted

    @Test
    fun `switching sort type from inverted NAME to RECENT shows most-recent-first`() = runTest {
        filterUiEnabledFlow.value = true
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Old", lastStart = 100L),
            smartScenario(id = 2, name = "New", lastStart = 300L),
        )
        sortSettingsFlow.value = ScenarioSortSettings(
            type = ScenarioSortType.NAME,
            inverted = true,
            showSmartScenario = true,
            showDumbScenario = true,
        )

        sortSettingsFlow.value = ScenarioSortSettings(
            type = ScenarioSortType.RECENT,
            inverted = false,
            showSmartScenario = true,
            showDumbScenario = true,
        )

        val names = scenarioItems(useCase(searchQueryFlow).first()).map { it.displayName }
        assertEquals(listOf("New", "Old"), names)
    }

    // endregion

    // region search query

    @Test
    fun `search query filters by name case-insensitively`() = runTest {
        filterUiEnabledFlow.value = true
        smartScenariosFlow.value = listOf(
            smartScenario(id = 1, name = "Alpha click"),
            smartScenario(id = 2, name = "Beta swipe"),
            smartScenario(id = 3, name = "ALPHA run"),
        )

        searchQueryFlow.value = "alpha"
        val result = useCase(searchQueryFlow).first()

        assertTrue(result.none { it is ScenarioListUiState.Item.SortItem })
        val names = scenarioItems(result).map { it.displayName }
        assertEquals(listOf("Alpha click", "ALPHA run"), names)
    }

    @Test
    fun `search query overrides sort and filter, returns no SortItem`() = runTest {
        filterUiEnabledFlow.value = true
        sortSettingsFlow.value = DEFAULT_SORT_SETTINGS.copy(showSmartScenario = false)
        smartScenariosFlow.value = listOf(smartScenario(id = 1, name = "Alpha"))

        searchQueryFlow.value = "alpha"
        val result = useCase(searchQueryFlow).first()

        assertTrue(result.none { it is ScenarioListUiState.Item.SortItem })
        assertEquals(1, scenarioItems(result).size)
    }

    @Test
    fun `clearing search query restores filter+sort mode`() = runTest {
        filterUiEnabledFlow.value = true
        smartScenariosFlow.value = listOf(smartScenario(id = 1, name = "Alpha"))

        searchQueryFlow.value = "alpha"
        searchQueryFlow.value = null
        val result = useCase(searchQueryFlow).first()

        assertTrue(result.any { it is ScenarioListUiState.Item.SortItem })
    }

    // endregion

    // region helpers

    private fun scenarioItems(items: List<ScenarioListUiState.Item>): List<ScenarioListUiState.Item.ScenarioItem> =
        items.filterIsInstance<ScenarioListUiState.Item.ScenarioItem>()

    private fun smartScenario(
        id: Long,
        name: String,
        lastStart: Long = 0L,
        startCount: Long = 0L,
    ): Scenario = mockk(relaxed = true) {
        every { this@mockk.id } returns Identifier(databaseId = id)
        every { this@mockk.name } returns name
        every { eventCount } returns 0
        every { stats } returns ScenarioStats(lastStartTimestampMs = lastStart, startCount = startCount)
    }

    private fun dumbScenario(
        id: Long,
        name: String,
        lastStart: Long = 0L,
        startCount: Long = 0L,
    ): DumbScenario = mockk(relaxed = true) {
        every { this@mockk.id } returns Identifier(databaseId = id)
        every { this@mockk.name } returns name
        every { dumbActions } returns emptyList()
        every { stats } returns ScenarioStats(lastStartTimestampMs = lastStart, startCount = startCount)
    }

    // endregion

    private companion object {
        val DEFAULT_SORT_SETTINGS = ScenarioSortSettings(
            type = ScenarioSortType.NAME,
            inverted = false,
            showSmartScenario = true,
            showDumbScenario = true,
        )
    }
}
