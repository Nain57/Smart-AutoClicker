/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.mainmenu

import android.content.Context
import android.content.SharedPreferences

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.engine.DetectorEngine
import com.buzbuz.smartautoclicker.engine.DetectorState
import com.buzbuz.smartautoclicker.overlays.utils.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.overlays.utils.getIsDebugReportEnabled
import com.buzbuz.smartautoclicker.overlays.utils.getIsDebugViewEnabled

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * View model for the [MainMenu].
 * @param context the Android context.
 */
class MainMenuModel(context: Context) : OverlayViewModel(context) {

    /** Debug configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getDebugConfigPreferences()
    /** The detector engine. */
    private var detectorEngine: DetectorEngine = DetectorEngine.getDetectorEngine(context)
    /** The repository for the scenarios. */
    private var repository: Repository? = Repository.getRepository(context)
    /** The current of the detection. */
    val detectionState: Flow<Boolean> = detectorEngine.state
        .map { it == DetectorState.DETECTING }
    /** The current list of event in the detector engine. */
    val eventList: Flow<List<Event>?> = detectorEngine.scenarioEvents

    /** Start/Stop the detection. */
    fun toggleDetection() {
        detectorEngine.apply {
            when (state.value) {
                DetectorState.DETECTING -> stopDetection()
                DetectorState.RECORDING -> startDetection(
                    sharedPreferences.getIsDebugViewEnabled(context),
                    sharedPreferences.getIsDebugReportEnabled(context),
                )
                else -> { /* Nothing to do */ }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository?.cleanCache()
        repository = null
    }
}