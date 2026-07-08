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
package com.buzbuz.smartautoclicker.core.processing.data.scaling

import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.extensions.ensureMinSize
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import javax.inject.Inject
import kotlin.math.max

class ScalingManager @Inject constructor(
    private val displayConfigManager: DisplayConfigManager,
) {

    private val conditionScalingInfo: MutableMap<Long, ScreenConditionScalingInfo> = mutableMapOf()

    private var detectionQuality: Double = QUALITY_MAX
    private var scalingRatio: Double = 1.0


    internal fun startScaling(quality: Double, screenEvents: List<ScreenEvent>): Point {
        detectionQuality = quality

        val scaledScreenSize = refreshScalingMetrics()
        refreshScalingData(scaledScreenSize, screenEvents.toConditionsList())

        return scaledScreenSize
    }

    internal fun refreshScaling(): Point {
        val scaledScreenSize = refreshScalingMetrics()
        refreshScalingData(scaledScreenSize, conditionScalingInfo.values.map { it.screenCondition })

        return scaledScreenSize
    }

    internal fun stopScaling() {
        detectionQuality = QUALITY_MAX

        val scaledScreenSize = refreshScalingMetrics()
        refreshScalingData(scaledScreenSize, emptyList())
    }

    internal fun getScreenConditionScalingInfo(imageCondition: ScreenCondition): ScreenConditionScalingInfo? =
        conditionScalingInfo[imageCondition.id.databaseId]

    internal fun scaleUpDetectionResult(result: Point): Point =
        result.scaleUp()


    private fun refreshScalingMetrics(): Point {
        val displaySize: Point = displayConfigManager.displayConfig.sizePx
        val biggestScreenSideSize: Int = max(displaySize.x, displaySize.y)

        scalingRatio =
            if (biggestScreenSideSize <= detectionQuality) 1.0
            else detectionQuality / biggestScreenSideSize

        val scaledScreenSize = displaySize.scaleDown()

        Log.i(TAG, "Scaling metrics refreshed: ratio=$scalingRatio, screenSize=$displaySize, " +
                "scaledScreenSize=$scaledScreenSize")

        return scaledScreenSize
    }

    private fun refreshScalingData(scaledScreenSize: Point, screenConditions: List<ScreenCondition>) {
        conditionScalingInfo.clear()

        screenConditions.forEach { screenCondition ->
            conditionScalingInfo[screenCondition.id.databaseId] = when (screenCondition) {
                is ScreenCondition.Color -> screenCondition.toColorScalingInfo(scaledScreenSize)
                is ScreenCondition.Image -> screenCondition.toImageScalingInfo(scaledScreenSize)
                is ScreenCondition.Number -> screenCondition.toNumberScalingInfo(scaledScreenSize)
                is ScreenCondition.Text -> screenCondition.toTextScalingInfo(scaledScreenSize)
            }
        }

        Log.i(TAG, "Scaling data refresh for ${screenConditions.size} conditions")
    }

    private fun ScreenCondition.Image.toImageScalingInfo(scaledScreenSize: Point): ScreenConditionScalingInfo.Image {
        val scaledImageArea = area.scaleDown()
        val bounds = scaledScreenSize.toArea()

        return ScreenConditionScalingInfo.Image(
            screenCondition = this,
            imageArea = scaledImageArea,
            detectionArea = when (detectionType) {
                EXACT -> scaledImageArea.grow(bounds)
                WHOLE_SCREEN -> bounds
                IN_AREA -> detectionArea?.scaleDown()?.grow(bounds)
                    ?: throw IllegalArgumentException("Invalid IN_AREA condition, no area defined")
                else -> throw IllegalArgumentException("Unexpected detection type")
            },
        )
    }

    private fun ScreenCondition.Color.toColorScalingInfo(scaledScreenSize: Point): ScreenConditionScalingInfo.Color =
        ScreenConditionScalingInfo.Color(
            screenCondition = this,
            detectionArea = detectionArea
                .scaleDown()
                .coerceIn(bounds = scaledScreenSize.toArea())
                .ensureMinSize()
        )

    private fun ScreenCondition.Text.toTextScalingInfo(scaledScreenSize: Point): ScreenConditionScalingInfo.Text =
        ScreenConditionScalingInfo.Text(
            screenCondition = this,
            detectionArea = detectionArea
                .scaleDown()
                .coerceIn(bounds = scaledScreenSize.toArea())
                .ensureMinSize()
        )

    private fun ScreenCondition.Number.toNumberScalingInfo(scaledScreenSize: Point): ScreenConditionScalingInfo.Number =
        ScreenConditionScalingInfo.Number(
            screenCondition = this,
            detectionArea = detectionArea
                .scaleDown()
                .coerceIn(bounds = scaledScreenSize.toArea())
                .ensureMinSize()
        )

    private fun List<ScreenEvent>.toConditionsList(): List<ScreenCondition> =
        fold(listOf()) { acc, event -> acc + event.conditions }

    private fun Point.scaleDown(): Point = scale(scalingRatio)
    private fun Point.scaleUp(): Point = scale(scalingRatio.inverseScalingRatio())
    private fun Rect.scaleDown(): Rect = scale(scalingRatio)
    private fun Double.inverseScalingRatio(): Double = 1.0 / this
}

private const val QUALITY_MAX = 10000.0
private const val TAG = "ScalingManager"