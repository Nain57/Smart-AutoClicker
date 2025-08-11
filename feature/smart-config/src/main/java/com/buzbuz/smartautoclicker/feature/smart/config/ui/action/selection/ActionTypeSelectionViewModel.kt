
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection

import android.view.View

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import javax.inject.Inject

class ActionTypeSelectionViewModel @Inject constructor(
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    fun monitorCreateClickView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION)
    }
}
