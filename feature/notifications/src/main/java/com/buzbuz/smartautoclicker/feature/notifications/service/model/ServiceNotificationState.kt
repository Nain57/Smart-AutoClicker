
package com.buzbuz.smartautoclicker.feature.notifications.service.model

internal data class ServiceNotificationState(
    val scenarioName: String,
    val isScenarioRunning: Boolean,
    val isMenuVisible: Boolean,
    val isNightMode: Boolean,
)