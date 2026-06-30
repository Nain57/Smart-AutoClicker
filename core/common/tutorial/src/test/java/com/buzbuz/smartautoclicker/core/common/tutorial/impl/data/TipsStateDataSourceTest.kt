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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.data

import android.content.Context
import android.os.Build

import androidx.test.core.app.ApplicationProvider

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tip

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TipsStateDataSourceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var dataSource: TipsStateDataSource

    @Before
    fun setUp() {
        dataSource = TipsStateDataSource(
            PreferencesDataStore(
                context = context,
                dispatcher = testDispatcher,
                fileName = UUID.randomUUID().toString(),
            )
        )
    }

    @Test
    fun `getTipsDontShowAgainValue returns false by default`() = runTest(testDispatcher) {
        val result = dataSource.getTipsDontShowAgainValue(Tip.STOP_WITH_VOLUME_DOWN).first()

        assertFalse(result)
    }

    @Test
    fun `setTipsDontShowAgain persists true for the tip`() = runTest(testDispatcher) {
        dataSource.setTipsDontShowAgain(Tip.STOP_WITH_VOLUME_DOWN)

        val result = dataSource.getTipsDontShowAgainValue(Tip.STOP_WITH_VOLUME_DOWN).first()
        assertTrue(result)
    }

    @Test
    fun `getTipsDontShowAgainValue is false before any write, true after`() = runTest(testDispatcher) {
        val before = dataSource.getTipsDontShowAgainValue(Tip.STOP_WITH_VOLUME_DOWN).first()
        assertFalse(before)

        dataSource.setTipsDontShowAgain(Tip.STOP_WITH_VOLUME_DOWN)

        val after = dataSource.getTipsDontShowAgainValue(Tip.STOP_WITH_VOLUME_DOWN).first()
        assertTrue(after)
    }
}
