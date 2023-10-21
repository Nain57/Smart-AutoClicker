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