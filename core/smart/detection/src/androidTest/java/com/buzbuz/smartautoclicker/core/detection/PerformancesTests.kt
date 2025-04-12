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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.buzbuz.smartautoclicker.core.detection.data.DetectionResolution
import com.buzbuz.smartautoclicker.core.detection.data.TestImage
import com.buzbuz.smartautoclicker.core.detection.utils.TEST_DETECTION_THRESHOLD_ALL
import com.buzbuz.smartautoclicker.core.detection.utils.loadTestBitmap
import com.buzbuz.smartautoclicker.core.detection.utils.setScreenMetrics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.math.max


/**
 * Execute global performances tests.
 * This class tests should always succeed, as they are generating csv results for manual analysis.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PerformancesTests {

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
    fun graph_Quality_To_Confidence() {
        val screenImage = TestImage.Screen.TutorialWithTarget
        val screenBitmap = context.loadTestBitmap(screenImage)
        val conditionImage = TestImage.Condition.TutorialTargetBlue
        val conditionBitmap = context.loadTestBitmap(conditionImage)

        val file = File("/sdcard/Download/Quality_To_Confidence.csv")
        BufferedWriter(FileWriter(file)).use { writer ->
            writer.writeDetectionResultsHeader()

            for (quality in DetectionResolution.MIN.value.toInt()..DetectionResolution.VERY_HIGH.value.toInt()) {
                testedDetector.apply {
                    setScreenMetrics(screenBitmap, quality.toDouble())
                    setupDetection(screenBitmap)

                    val confidence = detectCondition(conditionBitmap, TEST_DETECTION_THRESHOLD_ALL).confidenceRate
                    writer.writeDetectionResults(
                        screenImage = screenImage,
                        quality = quality,
                        confidence = confidence,
                    )
                }
            }
        }
    }

    private fun BufferedWriter.writeDetectionResultsHeader() {
        write("ScaleRatio,Confidence\n")
    }

    private fun BufferedWriter.writeDetectionResults(
        screenImage: TestImage.Screen,
        quality: Int,
        confidence: Double,
    ) {
        val scaleRatio = quality.toScaleRatio(screenImage)

        write("$scaleRatio,$confidence\n")
    }

    private fun Int.toScaleRatio(screenImage: TestImage.Screen): Double {
        val maxSize = max(screenImage.size.x, screenImage.size.y)
        return if (maxSize <= this) 1.0
        else this.toDouble() / maxSize
    }
}
