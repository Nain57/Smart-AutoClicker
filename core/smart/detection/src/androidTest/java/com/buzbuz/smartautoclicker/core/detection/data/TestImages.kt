
package com.buzbuz.smartautoclicker.core.detection.data

import android.graphics.Point
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
                    centerPosition = Point(672, 1696),
                    quality = 0.9750,
                ),
            )
        )
    }
}

internal data class ExpectedDetectionResults(
    val centerPosition: Point,
    val quality: Double,
)
