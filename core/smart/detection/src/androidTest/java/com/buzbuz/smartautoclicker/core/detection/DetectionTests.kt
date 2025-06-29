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
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class DetectionTests {

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
    fun detection_Screen1_Condition1_WholeScreen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue

        // When
        val result = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = Rect(0, 0, screenImage.size.x, screenImage.size.y),
        )

        // Then
        assertTrue("Image detection failed", result?.isValid() == true)
    }

    @Test
    fun detection_Screen1_Condition1_InArea() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val validArea = getValidCustomDetectionArea(screenImage, conditionImage)

        // When
        val result = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = validArea,
        )

        // Then
        assertTrue("Image detection failed", result?.isValid() == true)
    }

    @Test
    fun detection_Screen1_Condition1_Exact() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val exactArea = getDetectionExactArea(screenImage, conditionImage)

        // When
        val result = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = exactArea,
        )

        // Then
        assertTrue("Image detection failed", result?.isValid() == true)
    }

    @Test
    fun detection_Detection_Area_Bigger_Than_Screen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val outOfScreenArea = getBiggerThanScreenDetectionArea(screenImage)

        // When
        val result = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = outOfScreenArea,
        )

        // Then
        assertTrue("Image detection failed", result?.isValid() == false)
    }

    @Test
    fun detection_Detection_Area_Inside_But_Wider_Than_Screen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val outOfScreenArea = getInsideButWiderThanDetectionArea(screenImage)

        // When
        val result = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = outOfScreenArea,
        )

        // Then
        assertTrue("Image detection failed", result?.isValid() == false)
    }

    @Test
    fun detection_Detection_Area_Inside_But_Taller_Than_Screen() {
        // Given
        val screenImage = TestImage.Screen.TutorialWithTarget
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val outOfScreenArea = getInsideButTallerThanDetectionArea(screenImage)

        // When
        val result = testedDetector.executeImageDetectionTest(
            context = context,
            screenImage = screenImage,
            conditionImage = conditionImage,
            area = outOfScreenArea,
        )

        // Then
        assertTrue("Image detection failed", result?.isValid() == false)
    }

    private fun ImageDetector.executeImageDetectionTest(
        context: Context,
        screenImage: TestImage.Screen,
        conditionImage: TestImage.Condition,
        area: Rect,
    ): TestResults? = conditionImage.expectedResults[screenImage]?.let { expectedResults ->
         val screenBitmap = context.loadTestBitmap(screenImage)
            val conditionBitmap = context.loadTestBitmap(conditionImage)

            setScreenBitmap(screenBitmap)

            val results = detectCondition(
                conditionBitmap = conditionBitmap,
                conditionWidth = conditionBitmap.width,
                conditionHeight = conditionBitmap.height,
                detectionArea = area,
                threshold = TEST_DETECTION_THRESHOLD_ALL,
            )

            TestResults(
                expectedCenterPosition = expectedResults.centerPosition,
                actualCenterPosition = Point(results.position),
                expectedConfidence = expectedResults.quality,
                actualConfidence = results.confidenceRate,
            )
        }

    private fun TestResults.print() {
        println("Confidence=$actualConfidence/$expectedConfidence; " +
                "Position=$actualCenterPosition/$expectedCenterPosition}; isValid=${isValid()}")
    }
}


