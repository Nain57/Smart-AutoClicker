/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.tests.processor

import com.buzbuz.smartautoclicker.core.detection.DetectionResult
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.processing.tests.processor.ProcessingTests.BitmapSupplier
import org.mockito.Mockito.`when`


internal suspend fun BitmapSupplier.mockBitmapProviding(testCondition: TestImageCondition) {
    `when`(getBitmap(testCondition.imageCondition)).thenReturn(testCondition.mockedBitmap)
}

internal fun ImageDetector.mockAllDetectionResult(testConditions: List<TestImageCondition>, areAllDetected: Boolean) {
    testConditions.forEach { testCondition ->
        mockDetectionResult(testCondition, areAllDetected)
    }
}

internal fun ImageDetector.mockDetectionResult(testCondition: TestImageCondition, isDetected: Boolean) {
    when (testCondition.imageCondition.detectionType) {
        EXACT -> mockExactDetectionResult(testCondition, isDetected)
        WHOLE_SCREEN -> mockWholeScreenDetectionResult(testCondition, isDetected)
        IN_AREA -> mockInAreaDetectionResult(testCondition, isDetected)
    }
}

private fun ImageDetector.mockExactDetectionResult(testCondition: TestImageCondition, isDetected: Boolean) {
    `when`(
        detectCondition(
            testCondition.mockedBitmap,
            testCondition.imageCondition.area,
            testCondition.imageCondition.threshold,
        )
    ).thenReturn(DetectionResult(isDetected))
}

private fun ImageDetector.mockWholeScreenDetectionResult(testCondition: TestImageCondition, isDetected: Boolean) {
    `when`(
        detectCondition(
            testCondition.mockedBitmap,
            testCondition.imageCondition.threshold,
        )
    ).thenReturn(DetectionResult(isDetected))
}

private fun ImageDetector.mockInAreaDetectionResult(testCondition: TestImageCondition, isDetected: Boolean) {
    `when`(
        detectCondition(
            testCondition.mockedBitmap,
            testCondition.imageCondition.detectionArea!!,
            testCondition.imageCondition.threshold,
        )
    ).thenReturn(DetectionResult(isDetected))
}