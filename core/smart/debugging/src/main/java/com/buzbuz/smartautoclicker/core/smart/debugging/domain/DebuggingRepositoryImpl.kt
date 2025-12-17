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

import com.buzbuz.smartautoclicker.core.smart.debugging.data.DebugConfigurationLocalDataSource
import com.buzbuz.smartautoclicker.core.smart.debugging.data.DebugReportLocalDataSource
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.DebugEngine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class DebuggingRepositoryImpl @Inject constructor(
    debugEngine: DebugEngine,
    private val debugConfigurationDataSource: DebugConfigurationLocalDataSource,
    private val debugReportDataSource: DebugReportLocalDataSource,
) : DebuggingRepository {


    override val isLiveDebugging: Flow<Boolean> = debugEngine.isDebuggingSession.map { isDebuggingSession ->
        isDebuggingSession && isDebugViewEnabled()
    }

    override val lastImageEventFulfilled: Flow<DebugLiveImageEventOccurrence?> =
        debugEngine.lastImageEventFulfilled

    override val isDebugReportAvailable: StateFlow<Boolean> =
        debugReportDataSource.isReportAvailable


    override fun isDebugViewEnabled(): Boolean =
        debugConfigurationDataSource.isDebugViewEnabled()

    override fun isDebugReportEnabled(): Boolean =
        debugConfigurationDataSource.isDebugReportEnabled()

    override fun setDebuggingConfig(debugView: Boolean, debugReport: Boolean) =
        debugConfigurationDataSource.setDebuggingConfig(
            debugView = debugView,
            debugReport = debugReport,
        )

    override suspend fun getLastReportOverview(): DebugReportOverview? =
        debugReportDataSource.readOverview()

    override suspend fun getLastReportEventsOccurrences(): List<DebugReportEventOccurrence> =
        debugReportDataSource.readMessages()
}