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
package com.buzbuz.smartautoclicker.feature.backup.data.smart

import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.database.DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.EventType
import com.buzbuz.smartautoclicker.feature.backup.data.base.CONDITION_BACKUP_MATCH_REGEX
import com.buzbuz.smartautoclicker.feature.backup.data.base.LEGACY_CONDITION_BACKUP_MATCH_REGEX
import com.buzbuz.smartautoclicker.feature.backup.data.base.SMART_SCENARIO_BACKUP_MATCH_REGEX
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupDataSource
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupSerializer
import com.buzbuz.smartautoclicker.feature.backup.data.base.backupFolderName
import com.buzbuz.smartautoclicker.feature.backup.data.base.scenarioBackupFileName

import java.io.File

internal class SmartBackupDataSource(
    private val appDataDir: File,
): ScenarioBackupDataSource<ScenarioBackup, CompleteScenario>(appDataDir) {

    /** Regex matching a scenario json file into its folder in a backup archive. */
    private val scenarioUnzipMatchRegex = SMART_SCENARIO_BACKUP_MATCH_REGEX.toRegex()
    /** Regex matching a condition file (png) into its folder in a backup archive. */
    private val conditionUnzipMatchRegex = CONDITION_BACKUP_MATCH_REGEX.toRegex()
    /** Regex matching a legacy condition file (raw pixels) into its folder in a backup archive. */
    private val legacyConditionUnzipMatchRegex = LEGACY_CONDITION_BACKUP_MATCH_REGEX.toRegex()

    var screenCompatWarning = false
        private set

    override val serializer: ScenarioBackupSerializer<ScenarioBackup> = ScenarioSerializer()

    override fun isScenarioBackupFileZipEntry(fileName: String): Boolean =
        fileName.matches(scenarioUnzipMatchRegex)

    override fun isScenarioBackupAdditionalFileZipEntry(fileName: String): Boolean =
        fileName.matches(conditionUnzipMatchRegex) || fileName.matches(legacyConditionUnzipMatchRegex)

    override fun getBackupAdditionalFilesPaths(scenario: CompleteScenario): Set<String> =
        buildSet {
            scenario.events.forEach { completeEvent ->
                if (completeEvent.event.type == EventType.IMAGE_EVENT) {
                    completeEvent.conditions.forEach { condition ->
                        if (condition.type == ConditionType.ON_IMAGE_DETECTED) add(condition.path!!)
                    }
                }
            }
        }

    override fun getBackupZipFolderName(scenario: CompleteScenario): String =
        scenario.backupFolderName()

    override fun getBackupFileName(scenario: CompleteScenario): String =
        scenario.scenarioBackupFileName()

    override fun createBackupFromScenario(scenario: CompleteScenario, screenSize: Point): ScenarioBackup =
        ScenarioBackup(
            scenario = scenario,
            screenWidth = screenSize.x,
            screenHeight = screenSize.y,
            version = DATABASE_VERSION,
        )

    override fun verifyExtractedBackup(backup: ScenarioBackup, screenSize: Point): CompleteScenario? {
        Log.i(TAG, "Verifying smart scenario ${backup.scenario.scenario.id}")

        backup.scenario.events.forEach { event ->
            if (event.actions.isEmpty()) {
                Log.w(TAG, "Invalid scenario, action list is empty.")
                return null
            }

            if (event.conditions.isEmpty()) {
                Log.w(TAG, "Invalid scenario, condition list is empty.")
                return null
            }

            if (event.event.type == EventType.IMAGE_EVENT) {
                if (event.conditions.find { condition -> !condition.isScreenCondition() } != null) {
                    Log.w(TAG, "Invalid scenario, condition list is invalid.")
                    return null
                }
            }

            if (event.event.type == EventType.TRIGGER_EVENT) {
                if (event.conditions.find { condition -> !condition.isTriggerCondition() } != null) {
                    Log.w(TAG, "Invalid scenario, condition list is invalid.")
                    return null
                }
            }

            event.conditions.forEach { condition ->
                if (condition.type == ConditionType.ON_IMAGE_DETECTED && (
                            condition.path == null || !File(appDataDir, condition.path!!).exists())) {
                    Log.w(TAG, "Invalid screen condition, ${condition.path} file does not exist.")
                    return null
                }
            }
        }

        if (!screenCompatWarning) {
            screenCompatWarning = screenSize != Point(backup.screenWidth, backup.screenHeight)
        }

        Log.i(TAG, "Smart scenario is valid, has warnings: $screenCompatWarning")
        return backup.scenario
    }

    override fun reset() {
        super.reset()
        screenCompatWarning = false
    }

    private fun ConditionEntity.isScreenCondition(): Boolean =
        when (type) {
            ConditionType.ON_COLOR_DETECTED,
            ConditionType.ON_IMAGE_DETECTED,
            ConditionType.ON_NUMBER_DETECTED,
            ConditionType.ON_TEXT_DETECTED -> true

            ConditionType.ON_COUNTER_REACHED,
            ConditionType.ON_BROADCAST_RECEIVED,
            ConditionType.ON_TIMER_REACHED -> false
        }

    private fun ConditionEntity.isTriggerCondition(): Boolean =
        when (type) {
            ConditionType.ON_COUNTER_REACHED,
            ConditionType.ON_BROADCAST_RECEIVED,
            ConditionType.ON_TIMER_REACHED -> true

            ConditionType.ON_COLOR_DETECTED,
            ConditionType.ON_IMAGE_DETECTED,
            ConditionType.ON_NUMBER_DETECTED,
            ConditionType.ON_TEXT_DETECTED -> false
        }
}

/** Tag for logs. */
private const val TAG = "SmartBackupEngine"