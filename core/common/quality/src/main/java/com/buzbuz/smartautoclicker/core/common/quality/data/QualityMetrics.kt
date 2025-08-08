
package com.buzbuz.smartautoclicker.core.common.quality.data


/**
 * The different metrics used to determine the current software quality felt by the user.
 *
 * @param lastServiceStartTimeMs Start time in milliseconds of the accessibility service.
 * @param lastScenarioStartTimeMs Start time of the last user selected scenario in the activity.
 * @param accessibilityLossCount The number of times we have lost the permission since the last troubleshooting dialog
 *                               display.
 * @param troubleshootingDisplayCount The number of times the troubleshooting dialog have been displayed.
 */
internal data class QualityMetrics(
    val lastServiceStartTimeMs: Long,
    val lastScenarioStartTimeMs: Long,
    val accessibilityLossCount: Int,
    val troubleshootingDisplayCount: Int
)

internal const val INVALID_TIME = -1L