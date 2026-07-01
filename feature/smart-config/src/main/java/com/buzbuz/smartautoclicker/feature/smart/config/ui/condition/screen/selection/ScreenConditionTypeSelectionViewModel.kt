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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection

import android.view.View

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import javax.inject.Inject

class ScreenConditionTypeSelectionViewModel @Inject constructor(
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    fun monitorScreenConditionTypeView(type: ScreenConditionTypeChoice, choiceView: View) {
        monitoredViewsManager.attach(
            monitoredView = choiceView,
            type = type.getMonitoredViewType(),
        )
    }

    fun stopScreenConditionTypeViewMonitoring(type: ScreenConditionTypeChoice) {
        monitoredViewsManager.detach(type.getMonitoredViewType())
    }

    fun stopAllViewsMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_COLOR)
        monitoredViewsManager.detach(MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_IMAGE)
        monitoredViewsManager.detach(MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_NUMBER)
        monitoredViewsManager.detach(MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_TEXT)
    }

    private fun ScreenConditionTypeChoice.getMonitoredViewType() : MonitoredViewType =
        when (this) {
            ScreenConditionTypeChoice.OnColorDetected -> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_COLOR
            ScreenConditionTypeChoice.OnImageDetected-> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_IMAGE
            ScreenConditionTypeChoice.OnNumberDetected -> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_NUMBER
            ScreenConditionTypeChoice.OnTextDetected -> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_TEXT
        }
}
