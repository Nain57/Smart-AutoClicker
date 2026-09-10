/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.processing.domain

/** Receives synchronous performance measurements for the Debug Report. */
interface DebugReportTimingListener {

    /** Record one completed condition check. Implementations must not allocate per call. */
    fun onConditionChecked(conditionId: Long, durationNs: Long, fulfilled: Boolean)

    /** Record one completed call to the active scenario processing loop. */
    fun onDetectionLoopProcessed(durationNs: Long)

    /** Record elapsed suspension caused specifically by the user-configured Execution Limiter. */
    fun onExecutionLimiterWaited(durationNs: Long)

    /** Report the synchronously captured duration of the complete report session. */
    fun onReportSessionEnded(sessionDurationNs: Long) = Unit
}
