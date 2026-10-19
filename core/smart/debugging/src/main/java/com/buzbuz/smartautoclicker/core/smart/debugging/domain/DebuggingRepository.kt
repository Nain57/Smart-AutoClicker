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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveHeldEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportCounterInitialValue
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface DebuggingRepository {

    /** Tells if a detection session is currently live debugged, iow, displaying the debug overlay while detecting. */
    val isLiveDebugging: Flow<Boolean>

    /** The last event that has been matched, with all interpreted conditions results. */
    val lastImageEventProcessed: Flow<DebugLiveEventOccurrence?>

    /** The event currently holding a pointer on the screen, updated each second of hold. Null if nothing is held. */
    val heldEvent: Flow<DebugLiveHeldEvent?>

    /** Tells if a debug report is available. */
    val isDebugReportAvailable: StateFlow<Boolean>

    /** Tells if the event statistics are shown in the debug view. */
    val isDebugViewEventStatsShown: Flow<Boolean>

    /** Tells if the event actions are shown in the debug view. */
    val isDebugViewActionsShown: Flow<Boolean>

    /** Tells if the held finger is shown in the debug view. */
    val isDebugViewHeldFingerShown: Flow<Boolean>

    /** Tells if the debugging overlay should be enabled while detecting. */
    suspend fun isDebugViewEnabled(): Boolean

    /** Tells if a debug report should be created while detecting. */
    suspend fun isDebugReportEnabled(): Boolean

    /** Replace the existing debug configuration settings with the provided ones. */
    fun setDebuggingConfig(debugView: Boolean, debugReport: Boolean)

    fun setDebugViewEventStatsShown(isShown: Boolean)

    fun setDebugViewActionsShown(isShown: Boolean)

    fun setDebugViewHeldFingerShown(isShown: Boolean)

    /** Read the last detection session report overview, if any. */
    fun getLastReportOverview(): Flow<DebugReportOverview?>

    /** Read the counters initial values from the last detection session report. Null if no report is available. */
    fun getLastReportCountersInitialValues(): Flow<List<DebugReportCounterInitialValue>?>

    /** Read the last detection session events occurrences. List will be empty no rapport is available. */
    fun getLastReportEventsOccurrences(): Flow<List<DebugReportEventOccurrence>?>
}