/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.overview

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.getDurationsNs
import java.math.BigDecimal
import java.math.RoundingMode

internal data class ExecutionLimiterIdleTime(
    val waitDurationNs: Long,
    val measuredDurationNs: Long,
)

/**
 * Build the share of detection-related time deliberately spent waiting for the Execution Limiter.
 * Event action phases are removed from the active processing duration so they cannot dilute the result.
 */
internal fun buildExecutionLimiterIdleTime(
    overview: DebugReportOverview,
    occurrences: List<DebugReportEventOccurrence>,
): ExecutionLimiterIdleTime? {
    val activeDurationNs = overview.activeDetectionDuration.inWholeNanoseconds
    val waitDurationNs = overview.executionLimiterWaitDuration.inWholeNanoseconds
    if (activeDurationNs < 0L || waitDurationNs < 0L) return null
    if (waitDurationNs == 0L) return ExecutionLimiterIdleTime(0L, activeDurationNs)

    var previousActionsCompletedAtNs: Long? = null
    var actionDurationNs = 0L
    for (occurrence in occurrences) {
        val durations = occurrence.getDurationsNs(previousActionsCompletedAtNs) ?: return null
        actionDurationNs = try {
            Math.addExact(actionDurationNs, durations.actionsDurationNs)
        } catch (_: ArithmeticException) {
            return null
        }
        previousActionsCompletedAtNs = occurrence.actionsCompletedAtNs
    }

    val activeNonActionDurationNs = (activeDurationNs - actionDurationNs).coerceAtLeast(0L)
    val measuredDurationNs = try {
        Math.addExact(waitDurationNs, activeNonActionDurationNs)
    } catch (_: ArithmeticException) {
        return null
    }
    return ExecutionLimiterIdleTime(waitDurationNs, measuredDurationNs)
}

internal fun ExecutionLimiterIdleTime.formatPercentage(): String {
    if (waitDurationNs == 0L || measuredDurationNs == 0L) return "0%"
    if (waitDurationNs == measuredDurationNs) return "100%"

    val percentage = BigDecimal.valueOf(waitDurationNs)
        .multiply(BigDecimal.valueOf(100L))
        .divide(BigDecimal.valueOf(measuredDurationNs), 8, RoundingMode.HALF_UP)
    if (percentage < BigDecimal("0.1")) return "<0.1%"

    val rounded = percentage.setScale(1, RoundingMode.HALF_UP).min(BigDecimal("99.9"))
    return "${rounded.toPlainString()}%"
}
