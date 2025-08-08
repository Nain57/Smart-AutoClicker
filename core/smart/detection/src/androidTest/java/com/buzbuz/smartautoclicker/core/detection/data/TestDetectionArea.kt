
package com.buzbuz.smartautoclicker.core.detection.data

import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min


internal fun getDetectionExactArea(screen: TestImage.Screen, condition: TestImage.Condition): Rect =
    condition.expectedResults[screen]?.let { expectedResults ->
        val halfConditionWidth = condition.size.x / 2
        val halfConditionHeight = condition.size.y / 2

        Rect(
            expectedResults.centerPosition.x - halfConditionWidth,
            expectedResults.centerPosition.y - halfConditionHeight,
            expectedResults.centerPosition.x + halfConditionWidth,
            expectedResults.centerPosition.y + halfConditionHeight,
        )
    } ?: throw IllegalArgumentException("Screen $screen is not expected in $condition")


internal fun getValidCustomDetectionArea(screen: TestImage.Screen, condition: TestImage.Condition): Rect =
    condition.expectedResults[screen]?.let { expectedResults ->
        val halfConditionWidth = condition.size.x / 2
        val halfConditionHeight = condition.size.y / 2

        Rect(
            max(0, expectedResults.centerPosition.x - halfConditionWidth - VALID_CUSTOM_AREA_OFFSET),
            max(0, expectedResults.centerPosition.y - halfConditionHeight - VALID_CUSTOM_AREA_OFFSET),
            min(expectedResults.centerPosition.x + halfConditionWidth + VALID_CUSTOM_AREA_OFFSET, screen.size.x),
            min(expectedResults.centerPosition.y + halfConditionHeight + VALID_CUSTOM_AREA_OFFSET, screen.size.y),
        )
    } ?: throw IllegalArgumentException("Screen $screen is not expected in $condition")

internal fun getBiggerThanScreenDetectionArea(screen: TestImage.Screen): Rect =
    Rect(-150, -180, screen.size.x + 142, screen.size.y + 247)

internal fun getInsideButWiderThanDetectionArea(screen: TestImage.Screen): Rect =
    Rect(15, 16, screen.size.x + 142, screen.size.y - 11)

internal fun getInsideButTallerThanDetectionArea(screen: TestImage.Screen): Rect =
    Rect(14, 17, screen.size.x - 12, screen.size.y + 247)

private const val VALID_CUSTOM_AREA_OFFSET = 100