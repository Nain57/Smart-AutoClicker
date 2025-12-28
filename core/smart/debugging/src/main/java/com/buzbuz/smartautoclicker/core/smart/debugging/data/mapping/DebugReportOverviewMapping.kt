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
package com.buzbuz.smartautoclicker.core.smart.debugging.data.mapping

import com.buzbuz.smartautoclicker.core.smart.debugging.debugReportOverview
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import kotlin.time.Duration.Companion.milliseconds

import com.buzbuz.smartautoclicker.core.smart.debugging.DebugReportOverview as ProtoDebugReportOverview


internal fun DebugReportOverview.toProtobuf(): ProtoDebugReportOverview =
    debugReportOverview {
        scenarioId = this@toProtobuf.scenarioId
        durationMs = this@toProtobuf.duration.inWholeMilliseconds
        frameCount = this@toProtobuf.frameCount
        averageFrameProcessingDurationMs = this@toProtobuf.averageFrameProcessingDuration.inWholeMilliseconds
        imageEventFulfilledCount = this@toProtobuf.imageEventFulfilledCount
        triggerEventFulfilledCount = this@toProtobuf.triggerEventFulfilledCount
        countersName.addAll(this@toProtobuf.counterNames)
    }

internal fun ProtoDebugReportOverview.toDomain(): DebugReportOverview =
    DebugReportOverview(
        scenarioId = scenarioId,
        duration = durationMs.milliseconds,
        frameCount = frameCount,
        averageFrameProcessingDuration = averageFrameProcessingDurationMs.milliseconds,
        imageEventFulfilledCount = imageEventFulfilledCount,
        triggerEventFulfilledCount = triggerEventFulfilledCount,
        counterNames = countersNameList.toSet(),
    )