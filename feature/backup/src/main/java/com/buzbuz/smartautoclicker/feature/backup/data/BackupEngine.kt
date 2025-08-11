
package com.buzbuz.smartautoclicker.feature.backup.data

import android.content.ContentResolver
import android.graphics.Point
import android.net.Uri
import android.util.Log

import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.feature.backup.data.smart.SmartBackupDataSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** [BackupEngine] internal implementation. */
internal class BackupEngine(appDataDir: File, private val contentResolver: ContentResolver) {

    private val smartBackupDataSource: SmartBackupDataSource = SmartBackupDataSource(appDataDir)

    /**
     * Creates a new backup file.
     *
     * @param zipFileUri the uri of the file to write the backup into. Must be retrieved using the DocumentProvider.
     * @param smartScenarios the scenarios to backup.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup progress.
     */
    suspend fun createBackup(
        zipFileUri: Uri,
        smartScenarios: List<CompleteScenario>,
        screenSize: Point,
        progress: BackupProgress,
    ) {
        Log.d(TAG, "Create backup: $zipFileUri for scenarios: $smartScenarios")
        smartBackupDataSource.reset()

        var currentProgress = 0
        progress.onProgressChanged(currentProgress, smartScenarios.size)

        // Create the zip file containing the scenarios and their events conditions.
        withContext(Dispatchers.IO) {
            try {
                ZipOutputStream(contentResolver.openOutputStream(zipFileUri)).use { zipStream ->

                    smartScenarios.forEach { completeScenario ->
                        Log.d(TAG, "Backup smart scenario ${completeScenario.scenario.id}")

                        smartBackupDataSource.addScenarioToZipFile(zipStream, completeScenario, screenSize)

                        currentProgress++
                        progress.onProgressChanged(currentProgress, smartScenarios.size)
                    }

                    progress.onCompleted(smartScenarios, 0, false)
                }
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while creating backup archive.")
                progress.onError()
            } catch (isEx: IllegalStateException) {
                Log.e(TAG, "Error while creating backup archive, target folder can't be written")
                progress.onError()
            } catch (secEx: SecurityException) {
                Log.e(TAG, "Error while creating backup archive, permission is denied")
                progress.onError()
            }
        }
    }

    /**
     * Loads a backup file.
     *
     * @param zipFileUri the uri of the file to load the backup from. Must be retrieved using the DocumentProvider.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup import progress.
     */
    suspend fun loadBackup(zipFileUri: Uri, screenSize: Point, progress: BackupProgress) {
        Log.i(TAG, "Load backup: $zipFileUri")

        smartBackupDataSource.reset()

        var currentProgress = 0
        progress.onProgressChanged(currentProgress, null)

        withContext(Dispatchers.IO) {
            try {
                ZipInputStream(contentResolver.openInputStream(zipFileUri)).use { zipStream ->
                    generateSequence { zipStream.nextEntry }
                        .forEach { zipEntry ->
                            if (zipEntry.isDirectory) return@forEach

                            Log.d(TAG, "Extracting file ${zipEntry.name}")
                            when {

                                smartBackupDataSource.extractFromZip(zipStream, zipEntry.name) -> {
                                    if (smartBackupDataSource.isScenarioBackupFileZipEntry(zipEntry.name)) {
                                        Log.d(TAG, "Smart scenario file ${zipEntry.name} extracted")

                                        currentProgress++
                                        progress.onProgressChanged(currentProgress, null)
                                    }
                                }

                                else -> Log.w(TAG, "Nothing found to handle zip entry ${zipEntry.name}")
                            }
                        }
                }

                progress.onVerification?.invoke()
                smartBackupDataSource.verifyExtractedScenarios(screenSize)

                Log.i(TAG, "Backup loading completed: $zipFileUri")
                Log.i(TAG, "Inserting extracted scenarios into database")

                progress.onCompleted(
                    smartBackupDataSource.validBackups,
                    smartBackupDataSource.failureCount,
                    smartBackupDataSource.screenCompatWarning,
                )
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while loading backup archive", ioEx)
                progress.onError()
            } catch (secEx: SecurityException) {
                Log.e(TAG, "Error while loading backup archive, permission is denied", secEx)
                progress.onError()
            } catch (iaEx: IllegalArgumentException) {
                Log.e(TAG, "Error while loading backup archive, file is invalid", iaEx)
                progress.onError()
            } catch (npEx: NullPointerException) {
                Log.e(TAG, "Error while loading backup archive, file path is null", npEx)
                progress.onError()
            }
        }
    }
}

/** Tag for logs. */
private const val TAG = "BackupEngine"