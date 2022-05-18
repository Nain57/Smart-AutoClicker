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

import android.content.Context
import android.graphics.Point
import android.net.Uri

import com.buzbuz.smartautoclicker.database.room.entity.CompleteScenario

/** Handles the import/export of backup files. */
interface BackupEngine {

    companion object {

        /**
         * Get a new backup engine.
         *
         * @param context the Android context.
         *
         * @return the backup engine.
         */
        fun newBackupEngine(context: Context): BackupEngine {
            return BackupEngineImpl(context.filesDir, context.contentResolver)
        }
    }

    /**
     * Creates a new backup file.
     *
     * @param zipFileUri the uri of the file to write the backup into. Must be retrieved using the DocumentProvider.
     * @param scenarios the scenarios to backup.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup progress.
     */
    suspend fun createBackup(
        zipFileUri: Uri,
        scenarios: List<CompleteScenario>,
        screenSize: Point,
        progress: BackupProgress,
    )

    /**
     * Loads a backup file.
     *
     * @param zipFileUri the uri of the file to load the backup from. Must be retrieved using the DocumentProvider.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup import progress.
     */
    suspend fun loadBackup(zipFileUri: Uri, screenSize: Point, progress: BackupProgress)

    /**
     * Notifies for a backup progress and state.
     *
     * @param onProgressChanged called for each scenario processed.
     * @param onCompleted called when the backup is completed.
     * @param onError called when the backup has encountered an error.
     * @param onVerification called when the backup is verifying the imported data.
     */
    class BackupProgress(
        val onProgressChanged: suspend (current: Int?, max: Int?) -> Unit,
        val onCompleted: suspend (success: List<CompleteScenario>, failureCount: Int, compatWarning: Boolean) -> Unit,
        val onError: suspend () -> Unit,
        val onVerification: (suspend () -> Unit)? = null,
    )
}