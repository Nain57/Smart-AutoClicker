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
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import javax.inject.Inject
import kotlin.math.max

class ScaleRatioManager @Inject constructor(
    private val displayConfigManager: DisplayConfigManager,
) {

    private var detectionQuality: Double = QUALITY_MAX

    private var scalingRatio: Double = 1.0

    val downscaleRatio: Double
        get() = scalingRatio
    val upscaleRatio: Double
        get() = 1.0 / scalingRatio


    fun setDetectionQuality(quality: Double) {
        if (detectionQuality == quality) return
        detectionQuality = quality

        val displaySize: Point = displayConfigManager.displayConfig.sizePx
        val biggestScreenSideSize: Int = max(displaySize.x, displaySize.y)

        scalingRatio =
            if (biggestScreenSideSize <= detectionQuality) 1.0
            else detectionQuality / biggestScreenSideSize
    }
}

internal const val QUALITY_MAX = 10000.0