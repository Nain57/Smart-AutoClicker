/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import javax.inject.Inject

/** Fixed-size aggregate for processing-loop measurements included in the Debug Report overview. */
internal class ProcessingTimingRecorder @Inject constructor() {

    var activeDetectionDurationNs: Long = 0L
        private set
    var executionLimiterWaitDurationNs: Long = 0L
        private set

    fun recordDetectionLoop(durationNs: Long) {
        activeDetectionDurationNs += durationNs
    }

    fun recordExecutionLimiterWait(durationNs: Long) {
        executionLimiterWaitDurationNs += durationNs
    }

    fun reset() {
        activeDetectionDurationNs = 0L
        executionLimiterWaitDurationNs = 0L
    }
}
