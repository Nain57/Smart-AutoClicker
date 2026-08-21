/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report

/** Aggregate timing statistics for one condition during a detection session. */
data class ConditionProfile(
    val conditionId: Long,
    val checkCount: Long,
    val fulfilledCount: Long,
    val totalDurationNs: Long,
    val minDurationNs: Long,
    val maxDurationNs: Long,
)
