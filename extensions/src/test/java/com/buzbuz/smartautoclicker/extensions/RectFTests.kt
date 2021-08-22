/*
 * Copyright (C) 2021 Nain57
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

import android.graphics.PointF
import android.graphics.RectF
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests for the extensions for [RectF]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RectFTests {

    /** Object under tests, initialized before each test. */
    private lateinit var testedRectF: RectF

    @Before
    fun setUp() {
        testedRectF = RectF(TEST_DATA_RECT)
    }

    @Test
    fun scaleOne_shouldNotMove() {
        val expectedCenter = PointF(testedRectF.centerX(), testedRectF.centerY())
        testedRectF.scale(1f, expectedCenter)

        assertEquals(expectedCenter, PointF(testedRectF.centerX(), testedRectF.centerY()))
    }

    @Test
    fun scaleOne_shouldHaveSameSize() {
        val expectedSize = PointF(testedRectF.width(), testedRectF.height())
        testedRectF.scale(1f, PointF(testedRectF.centerX(), testedRectF.centerY()))

        assertEquals(expectedSize, PointF(testedRectF.width(), testedRectF.height()))
    }

    @Test
    fun scaleUp_pivotCenter() {
        val expectedCenter = PointF(testedRectF.centerX(), testedRectF.centerY())
        testedRectF.scale(2f, expectedCenter)

        assertEquals(expectedCenter, PointF(testedRectF.centerX(), testedRectF.centerY()))
    }

    @Test
    fun scaleUp_pivotLeft() {
        val expectedValue = testedRectF.left
        testedRectF.scale(2f, PointF(expectedValue, testedRectF.centerY()))

        assertEquals(expectedValue, testedRectF.left)
    }

    @Test
    fun scaleUp_pivotRight() {
        val expectedValue = testedRectF.right
        testedRectF.scale(2f, PointF(expectedValue, testedRectF.centerY()))

        assertEquals(expectedValue, testedRectF.right)
    }

    @Test
    fun scaleUp_pivotTop() {
        val expectedValue = testedRectF.top
        testedRectF.scale(2f, PointF(testedRectF.centerX(), expectedValue))

        assertEquals(expectedValue, testedRectF.top)
    }

    @Test
    fun scaleUp_pivotBottom() {
        val expectedValue = testedRectF.bottom
        testedRectF.scale(2f, PointF(testedRectF.centerX(), expectedValue))

        assertEquals(expectedValue, testedRectF.bottom)
    }

    @Test
    fun scaleUp_size() {
        val scaleFactor = 2f
        val expectedSize = PointF(testedRectF.width() * scaleFactor, testedRectF.height() * scaleFactor)
        testedRectF.scale(scaleFactor, PointF(testedRectF.centerX(), testedRectF.centerY()))

        assertEquals(expectedSize, PointF(testedRectF.width(), testedRectF.height()))
    }

    @Test
    fun scaleDown_pivotCenter() {
        val expectedCenter = PointF(testedRectF.centerX(), testedRectF.centerY())
        testedRectF.scale(0.8f, expectedCenter)

        assertEquals(expectedCenter, PointF(testedRectF.centerX(), testedRectF.centerY()))
    }

    @Test
    fun scaleDown_pivotLeft() {
        val expectedValue = testedRectF.left
        testedRectF.scale(0.8f, PointF(expectedValue, testedRectF.centerY()))

        assertEquals(expectedValue, testedRectF.left)
    }

    @Test
    fun scaleDown_pivotRight() {
        val expectedValue = testedRectF.right
        testedRectF.scale(0.8f, PointF(expectedValue, testedRectF.centerY()))

        assertEquals(expectedValue, testedRectF.right)
    }

    @Test
    fun scaleDown_pivotTop() {
        val expectedValue = testedRectF.top
        testedRectF.scale(0.8f, PointF(testedRectF.centerX(), expectedValue))

        assertEquals(expectedValue, testedRectF.top)
    }

    @Test
    fun scaleDown_pivotBottom() {
        val expectedValue = testedRectF.bottom
        testedRectF.scale(0.8f, PointF(testedRectF.centerX(), expectedValue))

        assertEquals(expectedValue, testedRectF.bottom)
    }

    @Test
    fun scaleDown_size() {
        val scaleFactor = 0.8f
        val expectedSize = PointF(testedRectF.width() * scaleFactor, testedRectF.height() * scaleFactor)
        testedRectF.scale(scaleFactor, PointF(testedRectF.centerX(), testedRectF.centerY()))

        assertEquals(expectedSize, PointF(testedRectF.width(), testedRectF.height()))
    }

    @Test
    fun translateSame() {
        val expectedArea = RectF(testedRectF)
        testedRectF.translate(0f, 0f)

        assertEquals(expectedArea, testedRectF)
    }

    @Test
    fun translateX_negative() {
        val translation = -10f
        val expectedLeft = testedRectF.left + translation
        val expectedRight = testedRectF.right + translation

        testedRectF.translate(translation, 0f)

        assertEquals(expectedLeft, testedRectF.left)
        assertEquals(expectedRight, testedRectF.right)
    }

    @Test
    fun translateX_positive() {
        val translation = 10f
        val expectedLeft = testedRectF.left + translation
        val expectedRight = testedRectF.right + translation

        testedRectF.translate(translation, 0f)

        assertEquals(expectedLeft, testedRectF.left)
        assertEquals(expectedRight, testedRectF.right)
    }

    @Test
    fun translateY_negative() {
        val translation = -10f
        val expectedTop = testedRectF.top + translation
        val expectedBottom = testedRectF.bottom + translation

        testedRectF.translate(0f, translation)

        assertEquals(expectedTop, testedRectF.top)
        assertEquals(expectedBottom, testedRectF.bottom)
    }

    @Test
    fun translateY_positive() {
        val translation = 10f
        val expectedTop = testedRectF.top + translation
        val expectedBottom = testedRectF.bottom + translation

        testedRectF.translate(0f, translation)

        assertEquals(expectedTop, testedRectF.top)
        assertEquals(expectedBottom, testedRectF.bottom)
    }
}

/** RectF used as reference rectangle for all tests. */
private val TEST_DATA_RECT = RectF(10f, 10f, 20f, 20f)