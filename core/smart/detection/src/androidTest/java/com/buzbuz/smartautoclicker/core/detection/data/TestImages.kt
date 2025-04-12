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
import android.graphics.Rect
import androidx.annotation.RawRes

import com.buzbuz.smartautoclicker.core.detection.test.R

/**
 * Describe the images used for the tests.
 *
 * @param fileRes the resource of the raw image file. Must be generated using the Klickr screen capture.
 * @param size the size of the image, in pixels.
 */
internal sealed class TestImage(@RawRes val fileRes: Int, val size: Point) {

    /** Describe the images used as screen capture. */
    sealed class Screen(fileRes: Int, size: Point) : TestImage(fileRes, size) {

        data object TutorialWithTarget : Screen(
            fileRes = R.raw.screen_1,
            size = Point(1344, 2992)
        )
    }

    /**
     * Describe the image used as condition images.
     * @param expectedResults map of [Screen] images their expected result with this condition.
     */
    sealed class Condition(
        fileRes: Int,
        size: Point,
        val expectedResults: Map<Screen, ExpectedDetectionResults>,
    ) : TestImage(fileRes, size) {

        data object TutorialTargetBlue : Condition(
            fileRes = R.raw.condition_1,
            size = Point(198, 192),
            expectedResults = mapOf(
                Screen.TutorialWithTarget to ExpectedDetectionResults(
                    area = Rect(573, 1600, 771, 1792),
                    resultsForQualities = getDefaultExpectedResultsForQualities(),
                ),
            )
        )
    }
}

internal data class ExpectedDetectionResults(
    val area: Rect,
    val resultsForQualities: Map<DetectionResolution, Double>,
)
