/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.type

import android.app.Application
import android.view.View

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.feature.tutorial.data.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.monitoring.TutorialMonitoredViewType

class ActionTypeSelectionViewModel(application: Application) : AndroidViewModel(application) {

    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    fun monitorCreateClickView(view: View) {
        println("TOTO: monitorCreateClickView $view")
        monitoredViewsManager.attach(TutorialMonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION, view)
    }

    fun stopViewMonitoring() {
        println("TOTO: stopViewMonitoring")
        monitoredViewsManager.detach(TutorialMonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION)
    }
}
