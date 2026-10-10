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
package com.buzbuz.smartautoclicker.core.settings.engine.data

import android.content.Context
import android.os.Build

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.workarounds.isImpactedByInputBlock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SettingsDataSourceTests {

    private lateinit var context: Context
    private lateinit var dataSource: SettingsDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataSource = SettingsDataSource(context, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        File(context.filesDir, "datastore").deleteRecursively()
    }

    @Test
    fun showOnLockScreen_defaultValue() = runTest {
        assertFalse(dataSource.showOnLockScreen().first())
    }

    @Test
    fun toggleShowOnLockScreen_notImpactedByInputBlock() = runTest {
        assertFalse(isImpactedByInputBlock())

        dataSource.toggleShowOnLockScreen()

        assertTrue(dataSource.showOnLockScreen().first())
    }

    @Test
    fun toggleShowOnLockScreen_twice() = runTest {
        dataSource.toggleShowOnLockScreen()
        dataSource.toggleShowOnLockScreen()

        assertFalse(dataSource.showOnLockScreen().first())
    }
}
