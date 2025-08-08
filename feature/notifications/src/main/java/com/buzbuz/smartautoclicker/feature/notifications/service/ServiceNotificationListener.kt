
package com.buzbuz.smartautoclicker.feature.notifications.service

import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationAction

interface ServiceNotificationListener {
    fun onPlay(): Unit?
    fun onPause(): Unit?
    fun onShow(): Unit?
    fun onHide(): Unit?
    fun onStop(): Unit?
}

internal fun ServiceNotificationListener.notifyAction(action: ServiceNotificationAction) =
    when (action) {
        ServiceNotificationAction.Play -> onPlay()
        ServiceNotificationAction.Pause -> onPause()
        ServiceNotificationAction.Show -> onShow()
        ServiceNotificationAction.Hide -> onHide()
        ServiceNotificationAction.Stop -> onStop()
        ServiceNotificationAction.Config -> Unit
    }