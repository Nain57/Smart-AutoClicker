/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.backup.data.base

import com.buzbuz.smartautoclicker.core.base.FILE_EXTENSION_JSON
import com.buzbuz.smartautoclicker.core.bitmaps.CONDITION_FILE_EXTENSION
import com.buzbuz.smartautoclicker.core.bitmaps.CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions


/** File extension for all scenario backup files. */
private const val SCENARIO_BACKUP_FILE_EXTENSION = FILE_EXTENSION_JSON

/**
 * File extension for all image condition backup files.
 * Note that before 3.3.7, condition files format was raw pixels and didn't have a file extension.
 */

private const val CONDITION_BACKUP_EXTENSION = CONDITION_FILE_EXTENSION

/** Prefix for all files in a Dumb Scenario backup. */
private const val DUMB_SCENARIO_BACKUP_FILE_PREFIX = "dumb-"



/** Get the name of the backup folder containing all backup files for dumb scenario. */
internal fun DumbScenarioWithActions.backupFolderName(): String =
    "$DUMB_SCENARIO_BACKUP_FILE_PREFIX${scenario.id}"

/** Get the name of the backup file containing the dumb scenario data. */
internal fun DumbScenarioWithActions.scenarioBackupFileName(): String =
    "${scenario.id}$SCENARIO_BACKUP_FILE_EXTENSION"

/**
 * Regex matching a condition file into its folder in a backup archive.
 * Will match any file like "scenarioId/dumb-scenarioId.json".
 */
internal const val DUMB_SCENARIO_BACKUP_MATCH_REGEX =
    """$DUMB_SCENARIO_BACKUP_FILE_PREFIX[0-9]+/[0-9]+$SCENARIO_BACKUP_FILE_EXTENSION"""



/** Get the name of the backup folder containing all backup files for smart scenario. */
internal fun CompleteScenario.backupFolderName(): String =
    "${scenario.id}"

/** Get the name of the backup file containing the smart scenario data. */
internal fun CompleteScenario.scenarioBackupFileName(): String =
    "${scenario.id}$SCENARIO_BACKUP_FILE_EXTENSION"

/**
 * Regex matching a smart scenario json file from its folder in a backup archive.
 * Will match any file like "scenarioId/scenarioId.json".
 */
internal const val SMART_SCENARIO_BACKUP_MATCH_REGEX =
    """[0-9]+/[0-9]+$SCENARIO_BACKUP_FILE_EXTENSION"""

/**
 * Regex matching a condition file into its folder in a backup archive.
 * Will match any file like "scenarioId/Condition_randomNumber.png".
 */
internal const val CONDITION_BACKUP_MATCH_REGEX =
    """[0-9]+/$CONDITION_FILE_PREFIX-?[0-9]+$CONDITION_BACKUP_EXTENSION"""

/**
 * Regex matching a legacy condition file into its folder in a backup archive.
 * Will match any file like "scenarioId/Condition_randomNumber".
 */
internal const val LEGACY_CONDITION_BACKUP_MATCH_REGEX =
    """[0-9]+/$CONDITION_FILE_PREFIX-?[0-9]+"""

