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
package com.buzbuz.smartautoclicker.feature.backup.data.dumb

import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.dumb.data.database.DUMB_DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.feature.backup.data.base.DUMB_SCENARIO_BACKUP_MATCH_REGEX
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupDataSource
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupSerializer
import com.buzbuz.smartautoclicker.feature.backup.data.base.backupFolderName
import com.buzbuz.smartautoclicker.feature.backup.data.base.scenarioBackupFileName

import java.io.File

internal class DumbBackupDataSource(
    appDataDir: File,
): ScenarioBackupDataSource<DumbScenarioBackup, DumbScenarioWithActions>(appDataDir) {

    /** Regex matching a condition file into its folder in a backup archive. */
    private val scenarioUnzipMatchRegex = DUMB_SCENARIO_BACKUP_MATCH_REGEX.toRegex()

    override val serializer: ScenarioBackupSerializer<DumbScenarioBackup> = DumbScenarioSerializer()

    override fun isScenarioBackupFileZipEntry(fileName: String): Boolean =
        fileName.matches(scenarioUnzipMatchRegex)

    override fun isScenarioBackupAdditionalFileZipEntry(fileName: String): Boolean =
        false

    override fun getBackupZipFolderName(scenario: DumbScenarioWithActions): String =
        scenario.backupFolderName()

    override fun getBackupFileName(scenario: DumbScenarioWithActions): String =
        scenario.scenarioBackupFileName()

    override fun createBackupFromScenario(scenario: DumbScenarioWithActions, screenSize: Point): DumbScenarioBackup =
        DumbScenarioBackup(
            dumbScenario = scenario,
            screenWidth = screenSize.x,
            screenHeight = screenSize.y,
            version = DUMB_DATABASE_VERSION,
        )

    override fun verifyExtractedBackup(backup: DumbScenarioBackup, screenSize: Point): DumbScenarioWithActions? {
        Log.i(TAG, "Verifying dumb scenario ${backup.dumbScenario.scenario.id}")

        if (backup.dumbScenario.dumbActions.isEmpty()) {
            Log.w(TAG, "Invalid dumb scenario, dumb action list is empty.")
            return null
        }

        return backup.dumbScenario
    }

    override fun getBackupAdditionalFilesPaths(scenario: DumbScenarioWithActions): Set<String> =
        emptySet()
}

/** Tag for logs. */
private const val TAG = "DumbBackupEngine"