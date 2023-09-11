/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.data

import androidx.annotation.CallSuper
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.ConditionProcessingDebugInfo
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.ProcessingDebugInfo

import kotlin.math.max
import kotlin.math.min

/** Record the processing of an event. */
internal open class Recorder {

    /** Record the timing for each processed event. */
    protected val processingTimingRecorder = TimingRecorder()
    /** The number of event processed. */
    protected var count: Long = 0L
        private set
    /** The number of event processed with a success. */
    var successCount: Long = 0L
        private set

    open fun onProcessingStart() {
        processingTimingRecorder.startRecord()
    }

    open fun onProcessingEnd(success: Boolean = true) {
        processingTimingRecorder.stopRecord()

        count++
        if (success) successCount++
    }

    open fun toProcessingDebugInfo() = ProcessingDebugInfo(
        processingCount = count,
        successCount = successCount,
        totalProcessingTimeMs = processingTimingRecorder.totalTimeMs,
        avgProcessingTimeMs = if (count != 0L) processingTimingRecorder.totalTimeMs / count else 0L,
        minProcessingTimeMs = processingTimingRecorder.minTimeMs,
        maxProcessingTimeMs = processingTimingRecorder.maxTimeMs,
    )

    @CallSuper
    open fun clear() {
        count = 0
        successCount = 0
        processingTimingRecorder.clear()
    }
}

/** Record the processing of a condition event. */
internal class ConditionRecorder : Recorder() {

    /** Record the confidence rate of positive detections. */
    private val detectionResultsRecorder = MinMaxTotalRecorder()

    fun onProcessingEnd(success: Boolean, newResult: Double?) {
        super.onProcessingEnd(success)
        if (newResult != null) detectionResultsRecorder.recordResult(newResult)
    }

    fun toConditionProcessingDebugInfo() = ConditionProcessingDebugInfo(
        processingCount = count,
        successCount = successCount,
        totalProcessingTimeMs = processingTimingRecorder.totalTimeMs,
        avgProcessingTimeMs = if (count != 0L) processingTimingRecorder.totalTimeMs / count else 0L,
        minProcessingTimeMs = processingTimingRecorder.minTimeMs,
        maxProcessingTimeMs = processingTimingRecorder.maxTimeMs,
        avgConfidenceRate = (detectionResultsRecorder.total / count).coerceIn(0.0, 100.0),
        minConfidenceRate = detectionResultsRecorder.min.coerceAtLeast(0.0),
        maxConfidenceRate = detectionResultsRecorder.max.coerceAtMost(100.0),
    )

    override fun clear() {
        super.clear()
        detectionResultsRecorder.clear()
    }

    override fun onProcessingEnd(success: Boolean) =
        throw UnsupportedOperationException("You must use onProcessingEnd(Boolean, Double?)")
    override fun toProcessingDebugInfo() =
        throw UnsupportedOperationException("You must use toConditionProcessingDebugInfo()")
}

/** Record the timings for an event. */
internal class TimingRecorder {

    /** The time in milliseconds of the last call to [startRecord]. */
    private var currentRecordStartTimeMs: Long = INVALID_TIME_VALUE

    /** The total processing time of all events. */
    var totalTimeMs: Long = 0L
        private set
    /** The minimum processing time of an event. */
    var minTimeMs: Long = Long.MAX_VALUE
        private set
    /** The maximum processing time of an event. */
    var maxTimeMs: Long = Long.MIN_VALUE
        private set

    fun startRecord() {
        currentRecordStartTimeMs = System.currentTimeMillis()
    }

    fun stopRecord() {
        val processingDurationMs =  System.currentTimeMillis() - currentRecordStartTimeMs
        currentRecordStartTimeMs = INVALID_TIME_VALUE

        totalTimeMs += processingDurationMs
        minTimeMs = min(processingDurationMs, minTimeMs)
        maxTimeMs = max(processingDurationMs, maxTimeMs)
    }

    fun clear() {
        currentRecordStartTimeMs = INVALID_TIME_VALUE
        totalTimeMs = 0L
        minTimeMs = Long.MAX_VALUE
        maxTimeMs = Long.MIN_VALUE
    }
}

/** Record the min, max and total values for an event. */
internal class MinMaxTotalRecorder {

    /** The total of all events  */
    var total: Double = 0.0
        private set
    /** The minimum of all events */
    var min: Double = Double.MAX_VALUE
        private set
    /** The maximum of all events */
    var max: Double = Double.MIN_VALUE
        private set

    fun recordResult(newResult: Double) {
        total += newResult
        min = min(newResult, min)
        max = max(newResult, max)
    }

    fun clear() {
        total = 0.0
        min = Double.MAX_VALUE
        max = Double.MIN_VALUE
    }
}

internal const val INVALID_TIME_VALUE = -1L