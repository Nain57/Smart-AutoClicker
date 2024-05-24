/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.domain

import android.content.Context
import android.content.SharedPreferences

import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener
import com.buzbuz.smartautoclicker.feature.smart.debugging.data.DebugEngine
import com.buzbuz.smartautoclicker.feature.smart.debugging.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.debugging.getIsDebugReportEnabled
import com.buzbuz.smartautoclicker.feature.smart.debugging.getIsDebugViewEnabled
import com.buzbuz.smartautoclicker.feature.smart.debugging.putIsDebugReportEnabled
import com.buzbuz.smartautoclicker.feature.smart.debugging.putIsDebugViewEnabled
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebuggingRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    /** Keep track of the scenario detection session. */
    private val debugEngine: DebugEngine = DebugEngine()
    /** Shared preferences containing the debug config. */
    private val sharedPreferences: SharedPreferences = context.getDebugConfigPreferences()

    /** Tells if a detection session is currently being debugged. */
    val isDebugging: Flow<Boolean> = debugEngine.isDebugging

    /** The debug report. Set once the detection session is complete. */
    val debugReport: Flow<DebugReport?> = debugEngine.debugReport

    /** The DebugInfo for the current image. */
    val lastResult: Flow<DebugInfo?> = debugEngine.currentInfo

    /** The DebugInfo for the last positive detection. */
    val lastPositiveInfo: Flow<DebugInfo?> = debugEngine.currentInfo
        .filter { it?.isDetected ?: false }

    /**
     * The listener upon scenario detection progress.
     * Must be set at detection start in order to get debugging info. If no debug features are enable, returns null.
     */
    fun getDebugDetectionListenerIfNeeded(context: Context): ScenarioProcessingListener? =
        if (isDebugViewEnabled(context) || isDebugReportEnabled(context)) debugEngine
        else null

    fun isDebugViewEnabled(context: Context): Boolean =
        sharedPreferences.getIsDebugViewEnabled(context)

    fun isDebugReportEnabled(context: Context): Boolean =
        sharedPreferences.getIsDebugReportEnabled(context)

    fun setDebuggingConfig(debugView: Boolean, debugReport: Boolean) =
        sharedPreferences
            .edit()
            .putIsDebugViewEnabled(debugView)
            .putIsDebugReportEnabled(debugReport)
            .apply()
}