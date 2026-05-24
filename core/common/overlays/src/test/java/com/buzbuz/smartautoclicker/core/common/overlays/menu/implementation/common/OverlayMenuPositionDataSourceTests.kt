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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common

import android.content.Context
import android.content.res.Configuration
import android.os.Build

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Test the [OverlayMenuPositionDataSource] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayMenuPositionDataSourceTests {

    private lateinit var context: Context
    private lateinit var dataSource: OverlayMenuPositionDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(OverlayMenuPositionDataSource.PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        dataSource = OverlayMenuPositionDataSource(context)
    }

    @Test
    fun loadLandscapePosition_withZeroX_returnsSavedPosition() {
        mockSavedLandscapePosition(x = 0, y = 42)

        val position = dataSource.loadMenuPosition(Configuration.ORIENTATION_LANDSCAPE)

        assertEquals(0, position?.x)
        assertEquals(42, position?.y)
    }

    @Test
    fun loadLandscapePosition_withZeroY_returnsSavedPosition() {
        mockSavedLandscapePosition(x = 84, y = 0)

        val position = dataSource.loadMenuPosition(Configuration.ORIENTATION_LANDSCAPE)

        assertEquals(84, position?.x)
        assertEquals(0, position?.y)
    }

    @Test
    fun loadLandscapePosition_withZeroXAndY_returnsSavedPosition() {
        mockSavedLandscapePosition(x = 0, y = 0)

        val position = dataSource.loadMenuPosition(Configuration.ORIENTATION_LANDSCAPE)

        assertEquals(0, position?.x)
        assertEquals(0, position?.y)
    }

    @Test
    fun loadLandscapePosition_withMissingKeys_returnsNull() {
        assertNull(dataSource.loadMenuPosition(Configuration.ORIENTATION_LANDSCAPE))
    }

    private fun mockSavedLandscapePosition(x: Int, y: Int) {
        context.getSharedPreferences(OverlayMenuPositionDataSource.PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(OverlayMenuPositionDataSource.PREFERENCE_MENU_X_LANDSCAPE_KEY, x)
            .putInt(OverlayMenuPositionDataSource.PREFERENCE_MENU_Y_LANDSCAPE_KEY, y)
            .commit()
    }
}
