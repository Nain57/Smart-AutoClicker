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
package com.buzbuz.smartautoclicker.detection.utils

import android.graphics.Rect

import com.buzbuz.smartautoclicker.database.ClickCondition
import com.buzbuz.smartautoclicker.database.ClickInfo

import java.lang.IllegalArgumentException

import kotlin.math.pow
import kotlin.math.sqrt

/** Test data and helpers for the ScenarioProcessor. */
internal object ProcessingData {

    /** Pixels displayed on the screen. Format is ARGB. */
    val SCREEN_PIXELS = intArrayOf(
        0x00111111, 0x00777777, 0x000077FF,
        0x00FF7700, 0x00ABCDEF, 0x00FEDCBA,
        0x00FFFFFF, 0x00123456, 0x00789ABC
    )

    /** The size of the screen. It is square so size = width = height. */
    val SCREEN_SIZE = sqrt(SCREEN_PIXELS.size.toDouble()).toInt()
    /** The total area of the screen. */
    val SCREEN_AREA = Rect(0, 0, SCREEN_SIZE, SCREEN_SIZE)

    /**
     * Get the pixel cache for a given area of the screen.
     * @param area the area on the screen.
     * @return the pixel cache, with first the screen pixels, second the empty array for processing
     */
    fun getScreenPixelCacheForArea(area: Rect): Pair<IntArray, IntArray> {
        if (area.width() > SCREEN_SIZE || area.height() > SCREEN_SIZE) {
            throw IllegalArgumentException("Invalid area, it is bigger than the screen")
        }

        // A rect 0,0,1,1 has a width of 1. We want the count of indexes, se we must add 1.
        val arraysSize = area.width() * area.height()
        val pixels = IntArray(arraysSize)
        var index = 0
        for (j in area.top until area.bottom) {
            for (i in area.left until area.right) {
                pixels[index] = SCREEN_PIXELS[j * SCREEN_SIZE + i]
                index++
            }
        }

        return pixels to IntArray(arraysSize)
    }

    /**
     * Get the pixel cache for a given area, but with other pixels than the ones on the screen.
     * @param area the area of the pixel cache.
     */
    fun getOtherPixelCacheForArea(area: Rect): Pair<IntArray, IntArray> {
        if (area.width() > SCREEN_SIZE || area.height() > SCREEN_SIZE) {
            throw IllegalArgumentException("Invalid area, it is bigger than the screen")
        }

        val arraysSize = (area.right - area.left + 1).toDouble().pow(2).toInt()
        return IntArray(arraysSize) to IntArray(arraysSize)
    }

    /** Instantiates a new click info with only the useful values for the tests. */
    fun newClickInfo(
        name: String,
        @ClickInfo.Companion.Operator operator: Int = ClickInfo.AND,
        conditions: List<ClickCondition> = emptyList()
    ) = ClickInfo(name).apply {
        conditionOperator = operator
        conditionList = conditions
    }
}