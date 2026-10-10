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
package com.buzbuz.smartautoclicker.settings

import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityRepository
import com.buzbuz.smartautoclicker.core.common.revenue.domain.IRevenueRepository
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val showOnLockScreenFlow = MutableStateFlow(false)

    private val mockQualityRepository: QualityRepository = mockk(relaxed = true)
    private val mockRevenueRepository: IRevenueRepository = mockk(relaxed = true)
    private val mockSettingsRepository: SettingsRepository = mockk(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        every { mockSettingsRepository.showOnLockScreenFlow } returns showOnLockScreenFlow
        viewModel = SettingsViewModel(
            qualityRepository = mockQualityRepository,
            revenueRepository = mockRevenueRepository,
            settingsRepository = mockSettingsRepository,
        )
    }

    @Test
    fun `isShowOnLockScreenEnabled is false when setting is disabled`() = runTest {
        showOnLockScreenFlow.value = false

        assertFalse(viewModel.isShowOnLockScreenEnabled.first())
    }

    @Test
    fun `isShowOnLockScreenEnabled is true when setting is enabled`() = runTest {
        showOnLockScreenFlow.value = true

        assertTrue(viewModel.isShowOnLockScreenEnabled.first())
    }

    @Test
    fun `toggleShowOnLockScreen toggles the setting`() {
        viewModel.toggleShowOnLockScreen()

        verify(exactly = 1) { mockSettingsRepository.toggleShowOnLockScreenFlow() }
    }
}
