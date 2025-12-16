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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report

/** Base class for the result of a condition processing during a session with debug report enabled. */
sealed class DebugReportConditionResult {

    /** The unique database identifier of the condition. */
    abstract val conditionId: Long
    /** Tells if the condition have been fulfilled or not. */
    abstract val isFulFilled: Boolean

    /** Result for a TriggerCondition. */
    data class TriggerCondition(
        override val conditionId: Long,
        override val isFulFilled: Boolean,
    ) : DebugReportConditionResult()

    /**
     * Result for an ImageCondition.
     *
     * @param detectionDurationMs The duration of the detection for this condition in milliseconds.
     * @param confidenceRate The confidence rate in percent (0-100) of the detection for this condition.
     */
    data class ImageCondition(
        override val conditionId: Long,
        override val isFulFilled: Boolean,
        val detectionDurationMs: Long,
        val confidenceRate: Double,
    ) : DebugReportConditionResult()
}