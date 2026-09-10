/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.ConditionProfile
import javax.inject.Inject

/** Fixed-size, allocation-free-on-record aggregate condition profiler. */
internal class ConditionProfileRecorder @Inject constructor() {

    private var conditionIds = LongArray(0)
    private var checkCounts = LongArray(0)
    private var fulfilledCounts = LongArray(0)
    private var totalDurationsNs = LongArray(0)
    private var minDurationsNs = LongArray(0)
    private var maxDurationsNs = LongArray(0)

    fun start(conditionIds: LongArray) {
        this.conditionIds = conditionIds.distinct().sorted().toLongArray()
        checkCounts = LongArray(this.conditionIds.size)
        fulfilledCounts = LongArray(this.conditionIds.size)
        totalDurationsNs = LongArray(this.conditionIds.size)
        minDurationsNs = LongArray(this.conditionIds.size) { Long.MAX_VALUE }
        maxDurationsNs = LongArray(this.conditionIds.size)
    }

    fun record(conditionId: Long, durationNs: Long, fulfilled: Boolean) {
        val index = conditionIds.binarySearch(conditionId)
        if (index < 0) return
        checkCounts[index] += 1
        if (fulfilled) fulfilledCounts[index] += 1
        totalDurationsNs[index] += durationNs
        if (durationNs < minDurationsNs[index]) minDurationsNs[index] = durationNs
        if (durationNs > maxDurationsNs[index]) maxDurationsNs[index] = durationNs
    }

    fun snapshot(): List<ConditionProfile> =
        buildList(conditionIds.size) {
            for (index in conditionIds.indices) {
                add(
                    ConditionProfile(
                        conditionId = conditionIds[index],
                        checkCount = checkCounts[index],
                        fulfilledCount = fulfilledCounts[index],
                        totalDurationNs = totalDurationsNs[index],
                        minDurationNs = minDurationsNs[index].takeUnless { it == Long.MAX_VALUE } ?: 0L,
                        maxDurationNs = maxDurationsNs[index],
                    )
                )
            }
        }

    fun reset() {
        conditionIds = LongArray(0)
        checkCounts = LongArray(0)
        fulfilledCounts = LongArray(0)
        totalDurationsNs = LongArray(0)
        minDurationsNs = LongArray(0)
        maxDurationsNs = LongArray(0)
    }
}
