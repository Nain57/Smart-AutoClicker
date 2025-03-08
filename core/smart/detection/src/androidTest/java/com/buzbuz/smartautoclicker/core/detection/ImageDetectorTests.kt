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
package com.buzbuz.smartautoclicker.core.detection

import android.content.Context
import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.buzbuz.smartautoclicker.core.detection.data.ActualDetectionResults
import com.buzbuz.smartautoclicker.core.detection.data.TestImage
import com.buzbuz.smartautoclicker.core.detection.data.isValid
import com.buzbuz.smartautoclicker.core.detection.utils.loadTestBitmap
import com.buzbuz.smartautoclicker.core.detection.utils.setScreenMetrics
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class ImageDetectorTests {

    private companion object {
        /** Allow to always returns the best match, even if not up to standards. */
        private const val TEST_DETECTION_THRESHOLD_ALL = 100
    }

    private lateinit var context: Context
    private lateinit var testedDetector: ImageDetector

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testedDetector = NativeDetector.newInstance() ?:
            throw IllegalStateException("Can't instantiate detector for tests")

        testedDetector.init()
    }

    @After
    fun tearDown() {
        testedDetector.close()
    }

    @Test
    fun verifyScreen1Condition1FullScreen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue

        // When
        val results = testedDetector.executeImageDetectionTest(
            screenImage = screenImage,
            conditionImage = conditionImage,
            threshold = TEST_DETECTION_THRESHOLD_ALL,
        )

        // Then
        results.verify()
    }

    private fun ImageDetector.executeImageDetectionTest(
        screenImage: TestImage.Screen,
        conditionImage: TestImage.Condition,
        threshold: Int,
    ): List<ActualDetectionResults> = conditionImage.expectedResults[screenImage]?.let { expectedResults ->
        buildList {
            val screenBitmap = context.loadTestBitmap(screenImage)
            val conditionBitmap = context.loadTestBitmap(conditionImage)

            expectedResults.resultsForQualities.forEach { (quality, expectedConfidence) ->
                setScreenMetrics(screenBitmap, quality.value)
                setupDetection(screenBitmap)

                val results = detectCondition(conditionBitmap, threshold)
                add(ActualDetectionResults(
                    resolution = quality,
                    expectedCenterPosition = expectedResults.centerPosition,
                    actualCenterPosition = Point(results.position),
                    expectedConfidence = expectedConfidence,
                    actualConfidence = results.confidenceRate,
                ))
            }
        }
    } ?: emptyList()

    private fun List<ActualDetectionResults>.verify() {
        var globalResult = true
        println("---------- Detection results START ----------  ")
        forEach { result ->
            result.print()
            globalResult = globalResult && result.isValid()
        }
        println("---------- Detection results END ----------  ")

        assertTrue("Image detection failed", globalResult)
    }

    private fun ActualDetectionResults.print() {
        println("$resolution(${resolution.value}): Confidence=$actualConfidence/$expectedConfidence; " +
                "Position=$actualCenterPosition/$expectedCenterPosition}; isValid=${isValid()}")

    }
}
