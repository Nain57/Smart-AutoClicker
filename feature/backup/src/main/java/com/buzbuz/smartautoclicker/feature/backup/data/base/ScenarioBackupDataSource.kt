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
package com.buzbuz.smartautoclicker.feature.backup.data.base

import android.graphics.Point
import android.util.Log
import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.feature.backup.data.ext.readAndCopyEntryFile
import com.buzbuz.smartautoclicker.feature.backup.data.ext.writeEntryFile

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal abstract class ScenarioBackupDataSource<Backup, BackupScenario>(private val appDataDir: File) {

    private val loadedBackups: MutableList<Backup> = mutableListOf()

    private val _validBackups: MutableList<BackupScenario> = mutableListOf()
    val validBackups: List<BackupScenario> = _validBackups

    var failureCount = 0
        private set


    protected abstract val serializer: ScenarioBackupSerializer<Backup>

    abstract fun isScenarioBackupFileZipEntry(fileName: String): Boolean

    abstract fun isScenarioBackupAdditionalFileZipEntry(fileName: String): Boolean

    protected abstract fun createBackupFromScenario(scenario: BackupScenario, screenSize: Point): Backup
    protected abstract fun verifyExtractedBackup(backup: Backup, screenSize: Point): BackupScenario?

    protected abstract fun getBackupFileName(scenario: BackupScenario): String
    protected abstract fun getBackupZipFolderName(scenario: BackupScenario): String
    protected abstract fun getBackupAdditionalFilesPaths(scenario: BackupScenario): Set<String>

    @CallSuper
    open fun reset() {
        loadedBackups.clear()
        _validBackups.clear()
        failureCount = 0
    }

    fun addScenarioToZipFile(zipStream: ZipOutputStream, scenario: BackupScenario, screenSize: Point) {
        Log.d(TAG, "Backup scenario $scenario")

        // Create json file from the data of the scenario
        val jsonFile = createScenarioJsonBackupFile(scenario, screenSize)

        // Add it to the archive and delete it.
        addScenarioFilesToZip(zipStream, scenario, jsonFile, getBackupAdditionalFilesPaths(scenario))
        jsonFile.delete()
    }

    private fun createScenarioJsonBackupFile(scenario: BackupScenario, screenSize: Point): File =
        File(appDataDir, getBackupFileName(scenario)).apply {
            Log.d(TAG, "Creating JSON backup file")
            if (exists()) {
                Log.w(TAG, "Backup file already exists, deleting previous one")
                delete()
                createNewFile()
            }

            outputStream().use { jsonOutStream ->
                serializer.serialize(createBackupFromScenario(scenario, screenSize), jsonOutStream)
            }
        }

    private fun addScenarioFilesToZip(
        zipStream: ZipOutputStream,
        scenario: BackupScenario,
        scenarioJsonFile: File,
        additionalFilesPaths: Set<String>,
    ) {
        val entryPrefix = "${getBackupZipFolderName(scenario)}/"
        Log.d(TAG, "Compress in folder $entryPrefix")

        zipStream.apply {
            // Create folder for the current scenario
            putNextEntry(ZipEntry(entryPrefix))

            // Put json file in the archive
            Log.d(TAG, "Add scenario JSON to archive")
            putNextEntry(
                ZipEntry("$entryPrefix${scenarioJsonFile.name}")
            )
            writeEntryFile(scenarioJsonFile)

            // Copy all additional files in the scenario folder of the zip archive
            additionalFilesPaths.forEach { additionalPath ->
                Log.d(TAG, "Add backup additional file to archive $additionalPath")
                putNextEntry(ZipEntry("$entryPrefix$additionalPath"))
                writeEntryFile(File(appDataDir, additionalPath))
            }
        }
    }

    fun extractFromZip(zipStream: ZipInputStream, fileName: String): Boolean {
        if (isScenarioBackupFileZipEntry(fileName)) {
            val backup = serializer.deserialize(zipStream)

            if (backup == null) {
                Log.w(TAG, "Can't deserialize $fileName")
                failureCount++
                return false
            }

            loadedBackups.add(backup)

            return true
        }

        if (!isScenarioBackupAdditionalFileZipEntry(fileName)) {
            return false
        }

        return extractAdditionalFileFromZip(zipStream, fileName)
    }

    /**
     * Extract a backup additional file from a zip file.
     *
     * @param zipStream the input stream the get the file from.
     * @param fileName the additional file name.
     */
    private fun extractAdditionalFileFromZip(zipStream: ZipInputStream, fileName: String): Boolean {
        val startIndex = fileName.lastIndexOf('/') + 1
        if (startIndex <= 0) {
            Log.w(TAG, "Invalid additional file path.")
            return false
        }

        val additionalFile = File(
            appDataDir,
            fileName.substring(startIndex),
        )

        if (additionalFile.exists()) {
            Log.d(TAG, "Additional file $fileName already exist, skip it.")
            return false
        }

        zipStream.readAndCopyEntryFile(additionalFile)
        return true
    }

    /**
     * Verifies if all smart scenario extracted are correctly formed.
     * This should be done after opening the whole zip file because conditions files are checked.
     */
    fun verifyExtractedScenarios(screenSize: Point) {
        loadedBackups.forEach { backup ->
            val scenario = verifyExtractedBackup(backup, screenSize)
            if (scenario != null) _validBackups.add(scenario)
            else failureCount++
        }
    }
}


const val SCENARIO_BACKUP_EXTENSION = ".json"
private const val TAG = "ScenarioBackupEngine"