/*
 * Copyright (C) 2026 Kevin Buzeau
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
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.buzbuz.smartautoclicker.core.detection.data.NumberTestCase
import com.buzbuz.smartautoclicker.core.detection.data.TestImage
import com.buzbuz.smartautoclicker.core.detection.utils.extractTestOcrModels
import com.buzbuz.smartautoclicker.core.detection.utils.loadTestBitmap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NumberMatcherTests {

    private lateinit var context: Context
    private lateinit var testedDetector: ImageDetector
    private lateinit var screenBitmap: Bitmap

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testedDetector = NativeDetector.newInstance()
            ?: throw IllegalStateException("Can't instantiate detector for tests")

        testedDetector.init()

        val (detectModelPath, recognitionModels) = context.extractTestOcrModels()
        val modelsLoaded = testedDetector.loadTextDetectionModels(detectModelPath, recognitionModels)
        assertTrue("OCR models failed to load", modelsLoaded)

        screenBitmap = context.loadTestBitmap(TestImage.NumberConditionsScreen)
        testedDetector.setScreenBitmap(screenBitmap, "")
    }

    @After
    fun tearDown() {
        testedDetector.close()
    }

    @Test
    fun detection_Number_42_Auto() {
        assertNumberDetected(TestImage.NumberConditionsScreen.numberTestCases[0])
    }

    @Test
    fun detection_Number_42dot5_DotDecimal() {
        assertNumberDetected(TestImage.NumberConditionsScreen.numberTestCases[1])
    }

    @Test
    fun detection_Number_42comma5_CommaDecimal() {
        assertNumberDetected(TestImage.NumberConditionsScreen.numberTestCases[2])
    }

    @Test
    fun detection_Number_42dot588_DotDecimal() {
        assertNumberDetected(TestImage.NumberConditionsScreen.numberTestCases[3])
    }

    @Test
    fun detection_Number_42comma588_CommaDecimal() {
        assertNumberDetected(TestImage.NumberConditionsScreen.numberTestCases[4])
    }

    @Test
    fun detection_Number_1dot234dot567comma890_CommaDecimal() {
        assertNumberDetected(TestImage.NumberConditionsScreen.numberTestCases[5])
    }

    @Test
    fun detection_Number_1comma234comma567dot890_DotDecimal() {
        assertNumberDetected(TestImage.NumberConditionsScreen.numberTestCases[6])
    }

    private fun assertNumberDetected(testCase: NumberTestCase) {
        val result = testedDetector.detectNumber(
            detectionArea = testCase.detectionArea,
            threshold = 0,
            numberFormatType = testCase.numberFormatType,
        )

        assertTrue("Number not detected in area ${testCase.detectionArea}", result.isDetected)
        assertNotNull("numberDetected is null for area ${testCase.detectionArea}", result.numberDetected)
        assertEquals(
            "Wrong value detected for area ${testCase.detectionArea}",
            testCase.expectedValue,
            result.numberDetected!!,
            DETECTION_NUMBER_DELTA,
        )
    }

    private companion object {
        const val DETECTION_NUMBER_DELTA = 0.001
    }
}
