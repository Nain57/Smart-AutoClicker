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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection

import android.view.View

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import javax.inject.Inject

class ActionTypeSelectionViewModel @Inject constructor(
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    fun monitorCreateClickView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION, view)
    }

    fun monitorCreateToggleEventView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.ACTION_TYPE_DIALOG_TOGGLE_EVENT_ACTION, view)
    }

    fun monitorCreateCounterView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.ACTION_TYPE_DIALOG_COUNTER_ACTION, view)
    }


    fun stopViewCreateClickMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION)
    }

    fun stopViewToggleEventMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.ACTION_TYPE_DIALOG_TOGGLE_EVENT_ACTION)
    }

    fun stopViewCounterMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.ACTION_TYPE_DIALOG_COUNTER_ACTION)
    }
}
