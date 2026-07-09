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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.selection

import android.view.View

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import javax.inject.Inject

class TriggerConditionTypeSelectionViewModel @Inject constructor(
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    fun monitorTriggerConditionTypeView(type: TriggerConditionTypeChoice, choiceView: View) {
        monitoredViewsManager.attach(
            monitoredView = choiceView,
            type = type.getMonitoredViewType(),
        )
    }

    fun stopTriggerConditionTypeViewMonitoring(type: TriggerConditionTypeChoice) {
        monitoredViewsManager.detach(type.getMonitoredViewType())
    }

    fun stopAllViewsMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_TIMER_REACHED)
        monitoredViewsManager.detach(MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_BROADCAST_RECEIVED)
        monitoredViewsManager.detach(MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_COUNTER_REACHED)
    }

    private fun TriggerConditionTypeChoice.getMonitoredViewType(): MonitoredViewType =
        when (this) {
            TriggerConditionTypeChoice.OnTimerReached -> MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_TIMER_REACHED
            TriggerConditionTypeChoice.OnBroadcastReceived -> MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_BROADCAST_RECEIVED
            TriggerConditionTypeChoice.OnCounterReached -> MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_COUNTER_REACHED
        }
}
