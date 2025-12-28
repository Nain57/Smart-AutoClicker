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

import kotlin.time.Duration

/**
 * Overview of the detection session.
 *
 * @param duration The duration of the detection session.
 * @param frameCount The number of frames processed during the session.
 * @param averageFrameProcessingDuration The average duration of a frame processing.
 * @param imageEventFulfilledCount The number of image events that have been triggered during the session.
 * @param triggerEventFulfilledCount The number of image events that have been triggered during the session.
 * @param counterNames The names of all counters available in the scenario that was ran to made this report.
 */
data class DebugReportOverview(
    val scenarioId: Long,
    val duration: Duration,
    val frameCount: Long,
    val averageFrameProcessingDuration: Duration,
    val imageEventFulfilledCount: Int,
    val triggerEventFulfilledCount: Int,
    val counterNames: Set<String>,
)