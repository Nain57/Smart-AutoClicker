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

import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.detection.DetectionResult
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResult
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ImageConditionScalingInfo
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener
import com.buzbuz.smartautoclicker.core.processing.tests.processor.ProcessingTests.BitmapSupplier
import com.buzbuz.smartautoclicker.core.processing.utils.anyNotNull
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify


internal fun ScalingManager.mockScaling(testCondition: TestImageCondition, detectionArea: Rect? = null) {
    `when`(getImageConditionScalingInfo(testCondition.imageCondition.id.databaseId)).thenReturn(
        ImageConditionScalingInfo(
            imageCondition = testCondition.imageCondition,
            imageArea = testCondition.imageCondition.area,
            detectionArea = detectionArea ?: testCondition.imageCondition.area,
        )
    )
}

internal suspend fun BitmapSupplier.mockBitmapProviding(testCondition: TestImageCondition) {
    `when`(getBitmap(
        testCondition.imageCondition.path,
        testCondition.imageCondition.area.width(),
        testCondition.imageCondition.area.height())
    ).thenReturn(testCondition.mockedBitmap)
}

internal fun ImageDetector.mockAllDetectionResult(testConditions: List<TestImageCondition>, areAllDetected: Boolean) {
    testConditions.forEach { testCondition ->
        mockDetectionResult(testCondition, areAllDetected)
    }
}

internal fun ImageDetector.mockDetectionResult(testCondition: TestImageCondition, isDetected: Boolean, detectionArea: Rect? = null) {
    `when`(
        detectCondition(
            conditionBitmap = testCondition.mockedBitmap,
            conditionWidth = testCondition.imageCondition.area.width(),
            conditionHeight = testCondition.imageCondition.area.height(),
            detectionArea = detectionArea ?: testCondition.imageCondition.area,
            threshold = testCondition.imageCondition.threshold,
        )
    ).thenReturn(DetectionResult(isDetected))
}

internal fun ImageDetector.verifyConditionNeverProcessed(testCondition: TestImageCondition) {
    verify(this, never())
        .detectCondition(
            conditionBitmap = eq(testCondition.mockedBitmap),
            conditionWidth = eq(testCondition.imageCondition.area.width()),
            conditionHeight = eq(testCondition.imageCondition.area.height()),
            detectionArea = any(),
            threshold = eq(testCondition.imageCondition.threshold),
        )
}

internal suspend fun ScenarioProcessingListener.verifyImageConditionProcessed(
    condition: TestImageCondition,
    detected: Boolean,
    processedCount: Int = 1,
): Unit = verify(this, times(processedCount))
    .onImageConditionProcessingCompleted(condition.expectedResult(detected))

internal suspend fun ScenarioProcessingListener.monitorImageEventProcessing(
    events: List<ImageEvent>,
): List<Boolean> {
    val results = mutableListOf<Boolean>()

    events.forEach { event ->
        `when`(onImageEventProcessingCompleted(eq(event), anyNotNull())).doAnswer { invocationOnMock ->
            results.add((invocationOnMock.arguments[1] as ConditionsResult).fulfilled == true)
            Unit
        }
    }

    return results
}

internal suspend fun ScenarioProcessingListener.verifyTriggerEventProcessed(
    event: TriggerEvent,
    expectedResult: Boolean,
    processedCount: Int = 1,
): Unit = verify(this, times(processedCount))
    .onTriggerEventProcessingCompleted(event, event.expectedResult(expectedResult))

internal suspend fun ScenarioProcessingListener.verifyTriggerEventNotProcessed(event: TriggerEvent) {
    verify(this, never()).onTriggerEventProcessingCompleted(event, event.expectedResult(true))
    verify(this, never()).onTriggerEventProcessingCompleted(event, event.expectedResult(false))
}