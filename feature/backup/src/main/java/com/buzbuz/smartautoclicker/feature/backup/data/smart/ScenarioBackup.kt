
package com.buzbuz.smartautoclicker.feature.backup.data.smart

import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario

import kotlinx.serialization.Serializable

/**
 * Represents a backup for a scenario.
 * This structure is used as the main element of the json file generated a export time. To keep backward/forward
 * compatibility, it should not be renamed, and its field should keep the same name (it is possible to add more fields).
 *
 * @param version the version of the backup.
 * @param screenWidth the width of the screen of the device that have generated this backup.
 * @param screenHeight the height of the screen of the device that have generated this backup.
 * @param scenario the scenario being exported/imported.
 */
@Serializable
internal data class ScenarioBackup(
    val version: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val scenario: CompleteScenario,
)