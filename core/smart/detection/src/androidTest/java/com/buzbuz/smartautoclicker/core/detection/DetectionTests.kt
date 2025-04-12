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
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.buzbuz.smartautoclicker.core.detection.data.TestResults
import com.buzbuz.smartautoclicker.core.detection.data.TestImage
import com.buzbuz.smartautoclicker.core.detection.data.getDetectionExactArea
import com.buzbuz.smartautoclicker.core.detection.data.getBiggerThanScreenDetectionArea
import com.buzbuz.smartautoclicker.core.detection.data.getInsideButTallerThanDetectionArea
import com.buzbuz.smartautoclicker.core.detection.data.getInsideButWiderThanDetectionArea
import com.buzbuz.smartautoclicker.core.detection.data.getValidCustomDetectionArea
import com.buzbuz.smartautoclicker.core.detection.data.isValid
import com.buzbuz.smartautoclicker.core.detection.utils.TEST_DETECTION_THRESHOLD_ALL
import com.buzbuz.smartautoclicker.core.detection.utils.loadTestBitmap
import com.buzbuz.smartautoclicker.core.detection.utils.setScreenMetrics
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class DetectionTests {

    private lateinit var context: Context
    private lateinit var testedDetector: ScreenDetector

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
    fun detection_Screen1_Condition1_WholeScreen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue

        // When
        val results = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
        )

        // Then
        results.verify()
    }

    @Test
    fun detection_Screen1_Condition1_InArea() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val validArea = getValidCustomDetectionArea(screenImage, conditionImage)

        // When
        val results = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = validArea,
        )

        // Then
        results.verify()
    }

    @Test
    fun detection_Screen1_Condition1_Exact() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val exactArea = getDetectionExactArea(screenImage, conditionImage)

        println("Detector: $exactArea")
        // When
        val results = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = exactArea,
        )

        // Then
        results.verify()
    }

    @Test
    fun detection_Detection_Area_Bigger_Than_Screen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val outOfScreenArea = getBiggerThanScreenDetectionArea(screenImage)

        // When
        val results = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = outOfScreenArea,
        )

        // Then
        results.verify()
    }

    @Test
    fun detection_Detection_Area_Inside_But_Wider_Than_Screen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val outOfScreenArea = getInsideButWiderThanDetectionArea(screenImage)

        // When
        val results = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = outOfScreenArea,
        )

        // Then
        results.verify()
    }

    @Test
    fun detection_Detection_Area_Inside_But_Taller_Than_Screen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val outOfScreenArea = getInsideButTallerThanDetectionArea(screenImage)

        // When
        val results = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = outOfScreenArea,
        )

        // Then
        results.verify()
    }

    private fun ScreenDetector.executeImageDetectionTest(
        context: Context,
        screenImage: TestImage.Screen,
        conditionImage: TestImage.Condition,
        area: Rect? = null,
    ): List<TestResults> = conditionImage.expectedResults[screenImage]?.let { expectedResults ->

        buildList {
            val screenBitmap = context.loadTestBitmap(screenImage)
            val conditionBitmap = context.loadTestBitmap(conditionImage)

            expectedResults.resultsForQualities.forEach { (quality, expectedConfidence) ->
                setScreenMetrics(screenBitmap, quality.value)
                setupDetection(screenBitmap)

                val results =
                    if (area != null) detectCondition(conditionBitmap, area, TEST_DETECTION_THRESHOLD_ALL)
                    else detectCondition(conditionBitmap, TEST_DETECTION_THRESHOLD_ALL)

                add(TestResults(
                    resolution = quality,
                    expectedArea = expectedResults.area,
                    actualArea = results.position,
                    expectedConfidence = expectedConfidence,
                    actualConfidence = results.confidenceRate,
                ))
            }
        }
    } ?: emptyList()

    private fun List<TestResults>.verify() {
        var globalResult = true
        println("---------- Detection results START ----------  ")
        forEach { result ->
            result.print()
            globalResult = globalResult && result.isValid()
        }
        println("---------- Detection results END ----------  ")

        assertTrue("Image detection failed", globalResult)
    }

    private fun TestResults.print() {
        println("$resolution(${resolution.value}): Confidence=$actualConfidence/$expectedConfidence; " +
                "Position=$actualArea/$expectedArea}; isValid=${isValid()}")

    }
}


