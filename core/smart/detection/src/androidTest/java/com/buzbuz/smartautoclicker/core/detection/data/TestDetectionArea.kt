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
    condition.expectedResults[screen]?.area
        ?: throw IllegalArgumentException("Screen $screen is not expected in $condition")


internal fun getValidCustomDetectionArea(screen: TestImage.Screen, condition: TestImage.Condition): Rect =
    condition.expectedResults[screen]?.area?.let { expectedArea ->
        Rect(
            max(0, expectedArea.left - VALID_CUSTOM_AREA_OFFSET),
            max(0, expectedArea.top - VALID_CUSTOM_AREA_OFFSET),
            min(expectedArea.right + VALID_CUSTOM_AREA_OFFSET, screen.size.x),
            min(expectedArea.bottom + VALID_CUSTOM_AREA_OFFSET, screen.size.y),
        )
    } ?: throw IllegalArgumentException("Screen $screen is not expected in $condition")

internal fun getBiggerThanScreenDetectionArea(screen: TestImage.Screen): Rect =
    Rect(-150, -180, screen.size.x + 142, screen.size.y + 247)

internal fun getInsideButWiderThanDetectionArea(screen: TestImage.Screen): Rect =
    Rect(15, 16, screen.size.x + 142, screen.size.y - 11)

internal fun getInsideButTallerThanDetectionArea(screen: TestImage.Screen): Rect =
    Rect(14, 17, screen.size.x - 12, screen.size.y + 247)

private const val VALID_CUSTOM_AREA_OFFSET = 100