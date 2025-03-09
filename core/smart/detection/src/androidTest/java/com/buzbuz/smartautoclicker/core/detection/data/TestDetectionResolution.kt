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
package com.buzbuz.smartautoclicker.core.detection.data

import com.buzbuz.smartautoclicker.core.detection.DETECTION_QUALITY_MIN


internal enum class DetectionResolution(val value: Double) {
    MAX(Double.MAX_VALUE),
    VERY_HIGH(2500.0),
    HIGH(2112.0),
    ABOVE_AVERAGE(1723.0),
    AVERAGE(1501.0),
    BELOW_AVERAGE(1262.0),
    LOW(1014.0),
    VERY_LOW(723.0),
    MIN(DETECTION_QUALITY_MIN.toDouble()),
}

internal fun getDefaultExpectedResultsForQualities(): Map<DetectionResolution, Double> = buildMap {
    DetectionResolution.entries.forEach { entry ->
        val expectedConfidence = when (entry) {
            DetectionResolution.MAX -> 0.9999
            DetectionResolution.VERY_HIGH -> 0.9900
            DetectionResolution.HIGH -> 0.9750
            DetectionResolution.ABOVE_AVERAGE -> 0.9700
            DetectionResolution.AVERAGE -> 0.9600
            DetectionResolution.BELOW_AVERAGE -> 0.9400
            DetectionResolution.LOW -> 0.9200
            DetectionResolution.VERY_LOW -> 0.9000
            DetectionResolution.MIN -> 0.8000
        }

        put(entry, expectedConfidence)
    }
}
