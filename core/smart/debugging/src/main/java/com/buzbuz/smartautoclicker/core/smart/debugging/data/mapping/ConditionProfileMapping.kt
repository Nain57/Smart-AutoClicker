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
package com.buzbuz.smartautoclicker.core.smart.debugging.data.mapping

import com.buzbuz.smartautoclicker.core.smart.debugging.ConditionProfileMessageKt.conditionProfileEntry
import com.buzbuz.smartautoclicker.core.smart.debugging.conditionProfileMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.debugReportMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.ConditionProfile
import com.buzbuz.smartautoclicker.core.smart.debugging.DebugReportMessage as ProtoDebugReportMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.ConditionProfileMessage as ProtoConditionProfileMessage

internal fun List<ConditionProfile>.toProtobuf(): ProtoDebugReportMessage =
    debugReportMessage {
        conditionProfileMessage = conditionProfileMessage {
            entries.addAll(this@toProtobuf.map { profile ->
                conditionProfileEntry {
                    conditionId = profile.conditionId
                    checkCount = profile.checkCount
                    fulfilledCount = profile.fulfilledCount
                    totalDurationNs = profile.totalDurationNs
                    minDurationNs = profile.minDurationNs
                    maxDurationNs = profile.maxDurationNs
                }
            })
        }
    }

internal fun ProtoConditionProfileMessage.toDomain(): List<ConditionProfile> =
    entriesList.map { entry ->
        ConditionProfile(
            conditionId = entry.conditionId,
            checkCount = entry.checkCount,
            fulfilledCount = entry.fulfilledCount,
            totalDurationNs = entry.totalDurationNs,
            minDurationNs = entry.minDurationNs,
            maxDurationNs = entry.maxDurationNs,
        )
    }
