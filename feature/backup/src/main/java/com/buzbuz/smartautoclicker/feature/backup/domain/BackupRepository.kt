
package com.buzbuz.smartautoclicker.feature.backup.domain

import android.content.Context
import android.graphics.Point
import android.net.Uri

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.feature.backup.data.BackupEngine
import com.buzbuz.smartautoclicker.feature.backup.data.BackupProgress
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val smartDatabase: ClickDatabase,
    private val smartRepository: IRepository,
) {

    private val backupEngine: BackupEngine = BackupEngine(
        appDataDir = context.filesDir,
        contentResolver = context.contentResolver,
    )

    /**
     * Create a backup of the provided scenario into the provided file.
     *
     * @param zipFileUri the uri of the file to write the backup into. Must be retrieved using the DocumentProvider.
     * @param smartScenarios the smart scenarios to backup.
     * @param screenSize the size of this device screen.
     *
     * @return a flow on the backup creation progress.
     */
    fun createScenarioBackup(
        zipFileUri: Uri,
        smartScenarios: List<Long>,
        screenSize: Point,
    ) = channelFlow {
        launch {
            backupEngine.createBackup(
                zipFileUri = zipFileUri,
                smartScenarios = smartScenarios.mapNotNull {
                    smartDatabase.scenarioDao().getCompleteScenario(it)
                },
                screenSize = screenSize,
                progress = BackupProgress(
                    onError = { send(Backup.Error) },
                    onProgressChanged = { current, max -> send(Backup.Loading(current, max)) },
                    onCompleted = { smarts, failureCount, compatWarning ->
                        send(Backup.Completed(
                            successCount = smarts.size,
                            failureCount = failureCount,
                            compatWarning = compatWarning,
                        ))
                    }
                )
            )
        }
    }

    /**
     * Restore a backup of scenarios from the provided file.
     *
     * @param zipFileUri the uri of the file to read the backup from. Must be retrieved using the DocumentProvider.
     * @param screenSize the size of this device screen.
     *
     * @return a flow on the backup import progress.
     */
    fun restoreScenarioBackup(zipFileUri: Uri, screenSize: Point) = channelFlow {
        launch {
            backupEngine.loadBackup(
                zipFileUri,
                screenSize,
                BackupProgress(
                    onError = { send(Backup.Error) },
                    onProgressChanged = { current, max -> send(Backup.Loading(current, max)) },
                    onVerification = { send(Backup.Verification) },
                    onCompleted = { smarts, failureCount, compatWarning ->
                        var totalFailures = failureCount

                        val smartsSuccess = smarts.toMutableList()
                        smarts.forEach { completeScenario ->
                            if (smartRepository.addScenarioCopy(completeScenario) == null) {
                                smartsSuccess.remove(completeScenario)
                                totalFailures++
                            }
                        }

                        send(Backup.Completed(
                            successCount = smartsSuccess.size,
                            failureCount = totalFailures,
                            compatWarning = compatWarning,
                        ))
                    }
                )
            )
        }
    }
}