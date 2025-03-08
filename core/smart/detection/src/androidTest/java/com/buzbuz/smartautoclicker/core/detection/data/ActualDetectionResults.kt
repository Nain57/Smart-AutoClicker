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

import android.graphics.Point

internal data class ActualDetectionResults(
    val resolution: DetectionResolution,
    val expectedCenterPosition: Point,
    val actualCenterPosition: Point,
    val expectedConfidence: Double,
    val actualConfidence: Double,
)


internal fun ActualDetectionResults.isValid(): Boolean =
    actualConfidence >= expectedConfidence && isCenterPositionValid(expectedCenterPosition, actualCenterPosition)

private fun isCenterPositionValid(expected: Point, actual: Point, delta: Int = TOLERATED_SCALING_ERROR_PIXELS): Boolean =
    isCenterPositionValid(expected.x, actual.x, delta) && isCenterPositionValid(expected.y, actual.y, delta)

private fun isCenterPositionValid(expected: Int, actual: Int, delta: Int) : Boolean =
    actual in (expected - delta)..(expected + delta)

private const val TOLERATED_SCALING_ERROR_PIXELS = 2