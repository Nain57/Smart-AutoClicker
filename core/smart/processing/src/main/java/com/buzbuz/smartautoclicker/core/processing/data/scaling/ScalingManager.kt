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

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import javax.inject.Inject
import kotlin.math.max

class ScalingManager @Inject constructor(
    private val displayConfigManager: DisplayConfigManager,
) {

    private val screenOrientationListener: (Context) -> Unit = { onOrientationChanged() }
    private val conditionScalingInfo: MutableMap<Long, ImageConditionScalingInfo> = mutableMapOf()

    private var detectionQuality: Double = QUALITY_MAX
    private var scalingRatio: Double = 1.0
    private var displaySizeListener: ((Point) -> Unit)? = null

    var scaledScreenSize: Point = Point()
        private set


    internal fun init(onDisplaySizeChanged: (Point) -> Unit) {
        displaySizeListener = onDisplaySizeChanged
        refreshScalingMetrics()

        displayConfigManager.addOrientationListener(screenOrientationListener)
    }

    internal fun startScaling(quality: Double, screenEvents: List<ImageEvent>) {
        detectionQuality = quality

        refreshScalingMetrics()
        refreshScalingData(screenEvents.fold(listOf()) { acc, event -> acc + event.conditions })

        displaySizeListener?.invoke(scaledScreenSize)
    }

    internal fun stopScaling() {
        detectionQuality = QUALITY_MAX

        refreshScalingMetrics()
        conditionScalingInfo.clear()
        displaySizeListener?.invoke(scaledScreenSize)
    }

    internal fun stop() {
        displayConfigManager.removeOrientationListener(screenOrientationListener)
        detectionQuality = QUALITY_MAX
        scalingRatio = 1.0
        displaySizeListener = null
    }

    internal fun getImageConditionScalingInfo(imageConditionId: Long): ImageConditionScalingInfo? =
        conditionScalingInfo[imageConditionId]

    internal fun scaleUpDetectionResult(result: Point): Point =
        result.scaleUp()

    private fun onOrientationChanged() {
        val oldScreenSize = scaledScreenSize

        refreshScalingMetrics()
        refreshScalingData(conditionScalingInfo.values.map { it.imageCondition })

        if (oldScreenSize != scaledScreenSize) displaySizeListener?.invoke(scaledScreenSize)
    }

    private fun refreshScalingMetrics() {
        val displaySize: Point = displayConfigManager.displayConfig.sizePx
        val biggestScreenSideSize: Int = max(displaySize.x, displaySize.y)

        scalingRatio =
            if (biggestScreenSideSize <= detectionQuality) 1.0
            else detectionQuality / biggestScreenSideSize

        scaledScreenSize = displaySize.scaleDown()

        Log.i(TAG, "Scaling metrics refreshed: ratio=$scalingRatio, screenSize=$displaySize, " +
                "scaledScreenSize=$scaledScreenSize")
    }

    private fun refreshScalingData(imageConditions: List<ImageCondition>) {
        conditionScalingInfo.clear()

        imageConditions.forEach { imageCondition ->
            val scaledImageArea = imageCondition.area.scaleDown()
            conditionScalingInfo[imageCondition.id.databaseId] =
                ImageConditionScalingInfo(
                    imageCondition = imageCondition,
                    imageArea = scaledImageArea,
                    detectionArea = imageCondition.getDetectionArea(
                        scaledImageArea = scaledImageArea,
                        bounds = scaledScreenSize.toArea(),
                    ),
                )
        }

        Log.i(TAG, "Scaling data refresh for ${imageConditions.size} conditions")
    }

    private fun ImageCondition.getDetectionArea(scaledImageArea: Rect, bounds: Rect): Rect =
        when (detectionType) {
            EXACT -> scaledImageArea.grow(bounds)
            WHOLE_SCREEN -> bounds
            IN_AREA -> detectionArea?.scaleDown()?.grow(bounds)
                ?: throw IllegalArgumentException("Invalid IN_AREA condition, no area defined")
            else -> throw IllegalArgumentException("Unexpected detection type")
        }


    private fun Point.scaleDown(): Point = scale(scalingRatio)
    private fun Point.scaleUp(): Point = scale(scalingRatio.inverseScalingRatio())
    private fun Rect.scaleDown(): Rect = scale(scalingRatio)
    private fun Double.inverseScalingRatio(): Double = 1.0 / this
}

private const val QUALITY_MAX = 10000.0
private const val TAG = "ScalingManager"