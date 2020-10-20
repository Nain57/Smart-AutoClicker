/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.extensions

import android.graphics.Point
import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests for the extensions for [Rect]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RectTests {

    @Test
    fun sizeZero() {
        val rect = Rect(0, 0, 0, 0)
        assertEquals(Point(0, 0), rect.size())
    }

    @Test
    fun sizePositiveValues() {
        val rect = Rect(10, 10, 110, 110)
        assertEquals(Point(100, 100), rect.size())
    }

    @Test
    fun sizeNegativeValues() {
        val rect = Rect(-100, -100, 100, 100)
        assertEquals(Point(200, 200), rect.size())
    }
}