
package com.buzbuz.smartautoclicker.core.processing.data.scaling

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

    private val conditionScalingInfo: MutableMap<Long, ImageConditionScalingInfo> = mutableMapOf()

    private var detectionQuality: Double = QUALITY_MAX
    private var scalingRatio: Double = 1.0


    internal fun startScaling(quality: Double, screenEvents: List<ImageEvent>): Point {
        detectionQuality = quality

        val scaledScreenSize = refreshScalingMetrics()
        refreshScalingData(scaledScreenSize, screenEvents.toConditionsList())

        return scaledScreenSize
    }

    internal fun refreshScaling(): Point {
        val scaledScreenSize = refreshScalingMetrics()
        refreshScalingData(scaledScreenSize, conditionScalingInfo.values.map { it.imageCondition })

        return scaledScreenSize
    }

    internal fun stopScaling() {
        detectionQuality = QUALITY_MAX

        val scaledScreenSize = refreshScalingMetrics()
        refreshScalingData(scaledScreenSize, emptyList())
    }

    internal fun getImageConditionScalingInfo(imageCondition: ImageCondition): ImageConditionScalingInfo? =
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

    private fun refreshScalingData(scaledScreenSize: Point, imageConditions: List<ImageCondition>) {
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

    private fun List<ImageEvent>.toConditionsList(): List<ImageCondition> =
        fold(listOf()) { acc, event -> acc + event.conditions }

    private fun Point.scaleDown(): Point = scale(scalingRatio)
    private fun Point.scaleUp(): Point = scale(scalingRatio.inverseScalingRatio())
    private fun Rect.scaleDown(): Rect = scale(scalingRatio)
    private fun Double.inverseScalingRatio(): Double = 1.0 / this
}

private const val QUALITY_MAX = 10000.0
private const val TAG = "ScalingManager"