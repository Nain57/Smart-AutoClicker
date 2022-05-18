/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.backup

import android.content.ContentResolver
import android.graphics.Point
import android.net.Uri
import android.util.Log

import com.buzbuz.smartautoclicker.backup.ext.readEntryFile
import com.buzbuz.smartautoclicker.backup.ext.writeEntryFile
import com.buzbuz.smartautoclicker.database.bitmap.CLICK_CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.database.room.entity.CompleteScenario

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** [BackupEngine] internal implementation. */
@Suppress("BlockingMethodInNonBlockingContext") // All are handled in IO dispatcher
internal class BackupEngineImpl(
    private val appDataDir: File,
    private val contentResolver: ContentResolver,
) : BackupEngine {

    /**
     * Regex matching a condition file into its folder in a backup archive.
     * Will match any file like "scenarioId/Condition_randomNumber".
     *
     * You can try it out here: https://regex101.com
     */
    private val conditionUnzipMatchRegex = """[0-9]+/$CLICK_CONDITION_FILE_PREFIX-?[0-9]+"""
        .toRegex()

    /** Serializer/Deserializer for database scenarios. */
    private val scenarioSerializer = ScenarioSerializer()

    override suspend fun createBackup(
        zipFileUri: Uri,
        scenarios: List<CompleteScenario>,
        screenSize: Point,
        progress: BackupEngine.BackupProgress,
    ) {
        Log.d(TAG, "Create backup: $zipFileUri for scenarios: $scenarios")

        var currentProgress = 0
        progress.onProgressChanged(currentProgress, scenarios.size)

        withContext(Dispatchers.IO) {

            // Create the zip file containing the scenarios and their events conditions.
            try {
                ZipOutputStream(contentResolver.openOutputStream(zipFileUri)).use { zipStream ->
                    scenarios.forEach { completeScenario ->
                        Log.d(TAG, "Backup scenario ${completeScenario.scenario.id}")

                        // Create json file from the data of the scenario
                        val jsonFile = createJsonBackupFile(completeScenario, screenSize)

                        // Add the json and all scenario conditions file to the archive
                        addScenarioToZip(zipStream, completeScenario.scenario.id, jsonFile, completeScenario.getConditionsPath())

                        // Delete the json file from the app data folder
                        jsonFile.delete()

                        // Increment the progress and notify
                        currentProgress++
                        progress.onProgressChanged(currentProgress, scenarios.size)
                    }

                    progress.onCompleted(scenarios, 0, false)
                }
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while creating backup archive.")
                progress.onError()
            }
        }
    }

    /**
     * Creates the json file for a scenario.
     *
     * @param completeScenario the scenario to create the json from.
     * @param screenSize this device screen size.
     *
     * @return the json backup file.
     */
    private fun createJsonBackupFile(completeScenario: CompleteScenario, screenSize: Point): File =
        File(appDataDir, "${completeScenario.scenario.id}$FILE_EXTENSION_JSON").apply {
            if (exists()) {
                Log.w(TAG, "Backup file already exists, deleting previous one")
                delete()
                createNewFile()
            }

            outputStream().use { jsonOutStream ->
                scenarioSerializer.serialize(completeScenario, screenSize, jsonOutStream)
            }
        }

    /**
     * Add a scenario to a zip file.
     *
     * @param zipStream the output stream on the zip file.
     * @param scenarioId the identifier of the scenario to be zipped.
     * @param jsonScenario the scenario to be added.
     * @param conditions the set of conditions path used by the scenario.
     */
    private fun addScenarioToZip(
        zipStream: ZipOutputStream,
        scenarioId: Long,
        jsonScenario: File,
        conditions: Set<String>,
    ) {
        val entryPrefix = "$scenarioId/"
        Log.d(TAG, "Compress in folder $entryPrefix")

        zipStream.apply {
            // Create folder for the current scenario
            putNextEntry(ZipEntry(entryPrefix))

            // Put json file in the archive
            putNextEntry(
                ZipEntry("$entryPrefix${jsonScenario.name}")
            )
            writeEntryFile(jsonScenario)

            // Put all conditions in the scenario folder in the archive
            conditions.forEach { conditionPath ->
                putNextEntry(ZipEntry("$entryPrefix$conditionPath"))
                writeEntryFile(File(appDataDir, conditionPath))
            }
        }
    }

    override suspend fun loadBackup(zipFileUri: Uri, screenSize: Point, progress: BackupEngine.BackupProgress) {
        Log.d(TAG, "Load backup: $zipFileUri")

        var screenCompatWarning = false
        var currentProgress = 0
        var failureCount = 0
        progress.onProgressChanged(0, null)

        val scenarioList = mutableListOf<CompleteScenario>()
        withContext(Dispatchers.IO) {
            try {
                ZipInputStream(contentResolver.openInputStream(zipFileUri)).use { zipStream ->
                    generateSequence { zipStream.nextEntry }
                        .forEach { zipEntry ->
                            when {
                                zipEntry.name.endsWith(FILE_EXTENSION_JSON) -> {
                                    Log.d(TAG, "Extract scenario file ${zipEntry.name}.")

                                    scenarioSerializer.deserialize(zipStream)?.let { backup ->
                                        scenarioList.add(backup.scenario)

                                        if (!screenCompatWarning) {
                                            screenCompatWarning =
                                                screenSize != Point(backup.screenWidth, backup.screenHeight)
                                        }

                                        currentProgress++
                                        progress.onProgressChanged(currentProgress, null)
                                    } ?:let {
                                        Log.w(TAG, "Can't deserialize ${zipEntry.name}.")
                                        failureCount++
                                    }
                                }

                                zipEntry.name.matches(conditionUnzipMatchRegex) -> {
                                    Log.d(TAG, "Extract condition file ${zipEntry.name}.")
                                    extractConditionFromZip(zipStream, zipEntry)
                                }

                                else -> { /* Ignore other files */ }
                            }
                        }
                }

                progress.onVerification?.invoke()
                val validScenarios = scenarioList.mapNotNull { scenario ->
                    if (verifyExtractedScenario(scenario)) {
                        scenario
                    } else {
                        failureCount++
                        Log.w(TAG, "Scenario ${scenario.scenario.id} is invalid.")
                        null
                    }
                }

                progress.onCompleted(validScenarios, failureCount, screenCompatWarning)
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while reading backup archive.")
                progress.onError()
            }
        }
    }

    /**
     * Extract a condition file from a zip file.
     *
     * @param zipStream the input stream the get the file from.
     * @param zipEntry the entry corresponding to the file.
     */
    private fun extractConditionFromZip(zipStream: ZipInputStream, zipEntry: ZipEntry) {
        val conditionFile = File(
            appDataDir,
            zipEntry.name.let { name ->
                val startIndex = name.lastIndexOf('/') + 1
                if (startIndex > 0) {
                    name.substring(startIndex)
                } else {
                    Log.w(TAG, "Invalid condition path.")
                    return
                }
            }
        )

        if (!conditionFile.exists()) {
            zipStream.readEntryFile(conditionFile)
        } else {
            Log.d(TAG, "Condition already exist, skip it.")
        }
    }

    /**
     * Verifies if the scenario extracted is correctly formed.
     * @param completeScenario the scenario to verify.
     * @return true if the scenario is correct, false if not.
     */
    private fun verifyExtractedScenario(completeScenario: CompleteScenario) : Boolean {
        Log.d(TAG, "Verifying scenario ${completeScenario.scenario.id}")

        completeScenario.events.forEach { completeEvent ->
            if (completeEvent.actions.isEmpty()) {
                Log.w(TAG, "Invalid scenario, action list is empty.")
                return false
            } else if (completeEvent.conditions.isEmpty()) {
                Log.w(TAG, "Invalid scenario, condition list is empty.")
                return false
            }

            completeEvent.conditions.forEach { condition ->
                if (!File(appDataDir, condition.path).exists()) {
                    Log.w(TAG, "Invalid condition, ${condition.path} file does not exist.")
                    return false
                }
            }
        }

        return true
    }

    /** @return the set of path in the app data directory for all conditions in this scenario. */
    private fun CompleteScenario.getConditionsPath() = buildSet {
        events.forEach { completeEvent ->
            completeEvent.conditions.forEach { condition ->
                add(condition.path)
            }
        }
    }
}

/** json file extension. */
private const val FILE_EXTENSION_JSON = ".json"
/** Tag for logs. */
private const val TAG = "BackupEngine"