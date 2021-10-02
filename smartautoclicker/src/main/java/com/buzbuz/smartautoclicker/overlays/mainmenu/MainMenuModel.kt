/*
 * Copyright (C) 2021 Nain57
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

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.detection.DetectorEngine

import kotlinx.coroutines.flow.Flow

/**
 * View model for the [MainMenu].
 * @param context the Android context.
 */
class MainMenuModel(context: Context) : OverlayViewModel(context) {

    /** The detector engine. */
    private var detectorEngine: DetectorEngine? = DetectorEngine.getDetectorEngine(context)
    /** The repository for the scenarios. */
    private var repository: Repository? = Repository.getRepository(context)
    /** The current of the detection. */
    val detectionState: Flow<Boolean> = detectorEngine!!.detecting
    /** The current list of event in the detector engine. */
    val eventList: Flow<List<Event>?> = detectorEngine!!.scenarioEvents

    /** Start/Stop the detection. */
    fun toggleDetection() {
        detectorEngine?.apply {
            if (detecting.value) {
                stopDetection()
            } else {
                startDetection()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository?.cleanCache()
        repository = null
        detectorEngine = null
    }
}