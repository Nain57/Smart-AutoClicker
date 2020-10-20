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

import android.graphics.RectF
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

import kotlin.math.pow

/** Tests for the extensions for [RectF]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RectFTests {

    private companion object {

        /** RectF used as reference rectangle for all tests. */
        private val TEST_DATA_RECT = RectF(10f, 10f, 20f, 20f)

        /**
         * Simple assertion on scaling.
         * If we scale the rectangle by two, we expect it to have its width and height enhanced times the scale ratio.
         * Given this information, we can simply calculate and compare their areas. So, the expected area will be:
         *
         *   width x scaleRatio x height x scaleRatio
         *
         * @param scaleRatio the ratio applied to the original data set.
         * @param actual the actual rect after the scaling to be verified.
         */
        fun assertScaled(scaleRatio: Float, actual: RectF) {
            val expectedArea = TEST_DATA_RECT.width() * TEST_DATA_RECT.height() * scaleRatio.pow(2)
            val actualArea = actual.width() * actual.height()
            assertEquals(expectedArea, actualArea)
        }
    }

    /** Object under tests, initialized before each test. */
    private lateinit var testedRectF: RectF

    @Before
    fun setUp() {
        testedRectF = RectF(TEST_DATA_RECT)
    }

    @Test
    fun scaleUp() {
        val scaleRatio = 2f
        testedRectF.scale(scaleRatio)
        assertScaled(scaleRatio, testedRectF)
    }

    @Test
    fun scaleDown() {
        val scaleRatio = 0.5f
        testedRectF.scale(scaleRatio)
        assertScaled(scaleRatio, testedRectF)
    }

    @Test
    fun scaleOne() {
        val scaleRatio = 1f
        testedRectF.scale(scaleRatio)
        assertScaled(scaleRatio, testedRectF)
    }

    @Test
    fun scaleZero() {
        val scaleRatio = 0f
        testedRectF.scale(scaleRatio)
        assertScaled(scaleRatio, testedRectF)
    }

    @Test
    fun moveSame() {
        testedRectF.move(testedRectF.centerX(), testedRectF.centerY())
        assertEquals(TEST_DATA_RECT, testedRectF)
    }

    @Test
    fun moveXOnly() {
        testedRectF.move(20f, TEST_DATA_RECT.centerY())
        assertEquals(RectF(15f, TEST_DATA_RECT.top, 25f,TEST_DATA_RECT.bottom), testedRectF)
    }

    @Test
    fun moveYOnly() {
        testedRectF.move(TEST_DATA_RECT.centerX(), 20f)
        assertEquals(RectF(TEST_DATA_RECT.left, 15f, TEST_DATA_RECT.right,25f), testedRectF)
    }

    @Test
    fun movePositive() {
        testedRectF.move(50f, 50f)
        assertEquals(RectF(45f, 45f, 55f,55f), testedRectF)
    }

    @Test
    fun moveNegative() {
        testedRectF.move(-50f, -50f)
        assertEquals(RectF(-55f, -55f, -45f,-45f), testedRectF)
    }
}