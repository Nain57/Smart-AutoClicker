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
package com.buzbuz.smartautoclicker.core.settings.engine

import com.buzbuz.smartautoclicker.core.settings.engine.data.ScenarioSortSettingsDataSource
import com.buzbuz.smartautoclicker.core.settings.engine.data.SettingsDataSource

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTests {

    private val showOnLockScreenFlow = MutableStateFlow(false)
    private val inputBlockWorkaroundFlow = MutableStateFlow(false)

    private val mockDataSource: SettingsDataSource = mockk(relaxed = true)
    private val mockSortDataSource: ScenarioSortSettingsDataSource = mockk()

    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        every { mockDataSource.isLegacyActionUiEnabled() } returns MutableStateFlow(false)
        every { mockDataSource.isLegacyNotificationUiEnabled() } returns MutableStateFlow(false)
        every { mockDataSource.isEntireScreenCaptureForced() } returns MutableStateFlow(false)
        every { mockDataSource.isFilterScenarioUiEnabled() } returns MutableStateFlow(false)
        every { mockDataSource.isInputBlockWorkaroundEnabled() } returns inputBlockWorkaroundFlow
        every { mockDataSource.showOnLockScreen() } returns showOnLockScreenFlow
        every { mockSortDataSource.getSortConfig() } returns emptyFlow()

        repository = SettingsRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            dataSource = mockDataSource,
            scenarioSortSettingsDatasource = mockSortDataSource,
        )
    }

    @Test
    fun `showOnLockScreen defaults to false`() {
        assertFalse(repository.showOnLockScreen())
    }

    @Test
    fun `showOnLockScreen reflects data source value`() {
        showOnLockScreenFlow.value = true

        assertTrue(repository.showOnLockScreen())
    }

    @Test
    fun `showOnLockScreenFlow reflects data source value`() = runTest {
        showOnLockScreenFlow.value = true

        assertTrue(repository.showOnLockScreenFlow.first())
    }

    @Test
    fun `showOnLockScreen is independent from input block workaround`() {
        inputBlockWorkaroundFlow.value = true

        assertFalse(repository.showOnLockScreen())
    }

    @Test
    fun `toggleShowOnLockScreenFlow toggles data source`() {
        repository.toggleShowOnLockScreenFlow()

        coVerify(exactly = 1) { mockDataSource.toggleShowOnLockScreen() }
        coVerify(exactly = 0) { mockDataSource.toggleInputBlockWorkaround() }
    }
}
