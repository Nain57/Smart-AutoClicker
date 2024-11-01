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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify


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
        EXACT -> `when`(
            detectCondition(
                testCondition.mockedBitmap,
                testCondition.imageCondition.area,
                testCondition.imageCondition.threshold,
            )
        ).thenReturn(DetectionResult(isDetected))

        WHOLE_SCREEN -> `when`(
            detectCondition(
                testCondition.mockedBitmap,
                testCondition.imageCondition.threshold,
            )
        ).thenReturn(DetectionResult(isDetected))

        IN_AREA -> `when`(
            detectCondition(
                testCondition.mockedBitmap,
                testCondition.imageCondition.detectionArea!!,
                testCondition.imageCondition.threshold,
            )
        ).thenReturn(DetectionResult(isDetected))
    }
}

internal fun ImageDetector.verifyConditionNeverProcessed(testCondition: TestImageCondition) {
    when (testCondition.imageCondition.detectionType) {
        EXACT -> verify(this, never())
            .detectCondition(
                testCondition.mockedBitmap,
                testCondition.imageCondition.area,
                testCondition.imageCondition.threshold,
            )

        WHOLE_SCREEN -> verify(this, never())
            .detectCondition(
                testCondition.mockedBitmap,
                testCondition.imageCondition.threshold,
            )

        IN_AREA -> verify(this, never())
            .detectCondition(
                testCondition.mockedBitmap,
                testCondition.imageCondition.detectionArea!!,
                testCondition.imageCondition.threshold,
            )
    }
}
