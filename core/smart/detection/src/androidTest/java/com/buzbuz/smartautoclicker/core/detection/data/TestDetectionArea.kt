/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.detection.data

import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min


internal fun getDetectionExactArea(screen: TestImage.Screen, condition: TestImage.Condition): Rect =
    condition.expectedResults[screen]?.let { expectedResults ->
        val halfConditionWidth = condition.size.x / 2
        val halfConditionHeight = condition.size.y / 2

        Rect(
            expectedResults.centerPosition.x - halfConditionWidth,
            expectedResults.centerPosition.y - halfConditionHeight,
            expectedResults.centerPosition.x + halfConditionWidth,
            expectedResults.centerPosition.y + halfConditionHeight,
        )
    } ?: throw IllegalArgumentException("Screen $screen is not expected in $condition")


internal fun getValidCustomDetectionArea(screen: TestImage.Screen, condition: TestImage.Condition): Rect =
    condition.expectedResults[screen]?.let { expectedResults ->
        val halfConditionWidth = condition.size.x / 2
        val halfConditionHeight = condition.size.y / 2

        Rect(
            max(0, expectedResults.centerPosition.x - halfConditionWidth - VALID_CUSTOM_AREA_OFFSET),
            max(0, expectedResults.centerPosition.y - halfConditionHeight - VALID_CUSTOM_AREA_OFFSET),
            min(expectedResults.centerPosition.x + halfConditionWidth + VALID_CUSTOM_AREA_OFFSET, screen.size.x),
            min(expectedResults.centerPosition.y + halfConditionHeight + VALID_CUSTOM_AREA_OFFSET, screen.size.y),
        )
    } ?: throw IllegalArgumentException("Screen $screen is not expected in $condition")

private const val VALID_CUSTOM_AREA_OFFSET = 100