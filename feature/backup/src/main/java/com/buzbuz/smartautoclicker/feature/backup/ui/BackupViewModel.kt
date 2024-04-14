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
package com.buzbuz.smartautoclicker.feature.backup.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.feature.backup.R
import com.buzbuz.smartautoclicker.feature.backup.domain.Backup
import com.buzbuz.smartautoclicker.feature.backup.domain.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model for the [BackupDialogFragment].
 * Handle the state of a backup, import or export.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    private val displayMetrics: DisplayMetrics,
) : ViewModel() {

    /** The state of the backup. Null if not started yet. */
    private val _backupState = MutableStateFlow<BackupDialogUiState?>(null)
    /** The current UI state of the backup. Null if not started yet. */
    val backupState: StateFlow<BackupDialogUiState?> = _backupState

    /**
     * Setup this view model by specifying if we want to import or export.
     * @param isImport true for import, false for export.
     */
    fun initialize(context: Context, isImport: Boolean) {
        _backupState.value = getInitialState(context, isImport)
    }

    /** @return the intent for selecting the file for the new exported backup. */
    fun createBackupFileCreationIntent() =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_TYPE_ZIP
            putExtra(Intent.EXTRA_TITLE, "SmartAutoClicker-Backup.zip")
        }

    /** @return the intent for selecting the file containing the imported backup. */
    fun createBackupRestorationFileSelectionIntent() =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_TYPE_ZIP
        }

    /**
     * Start the backup.
     *
     * @param uri the uri of the file provided by [createBackupFileCreationIntent] or
     * [createBackupRestorationFileSelectionIntent] intent data result.
     * @param isImport true for an import, false for an export.
     * @param smartScenarios the list of scenario to be exported. Ignored for an import.
     */
    fun startBackup(
        context: Context,
        uri: Uri,
        isImport: Boolean,
        dumbScenarios: List<Long> = emptyList(),
        smartScenarios: List<Long> = emptyList(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isImport) {
                repository.restoreScenarioBackup(uri, displayMetrics.screenSize).collect { backup ->
                    updateBackupState(context, backup, true)
                }
            } else {
                repository.createScenarioBackup(uri, dumbScenarios, smartScenarios, displayMetrics.screenSize).collect { backup ->
                    updateBackupState(context, backup, false)
                }
            }
        }
    }

    /**
     * Update the backup with the last results.
     * @param backup the last backup results.
     * @param isImport true for an import, false for an export.
     */
    private fun updateBackupState(context: Context, backup: Backup?, isImport: Boolean) {
        _backupState.value = when (backup) {
            is Backup.Loading -> getLoadingState(context, backup, isImport)
            Backup.Verification -> getVerificationState(context)
            Backup.Error -> getErrorState(context, isImport)
            is Backup.Completed -> getCompletedState(context, backup, isImport)
            null -> getInitialState(context, isImport)
        }
    }

    /**
     * Get the initial UI state.
     * @param isImport true for an import, false for an export.
     * @return the initial state.
     */
    private fun getInitialState(context: Context, isImport: Boolean) = BackupDialogUiState(
        fileSelectionVisibility = View.VISIBLE,
        loadingVisibility = View.GONE,
        textStatusVisibility = View.GONE,
        compatWarningVisibility = View.GONE,
        iconStatusVisibility = View.VISIBLE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = true,
        fileSelectionText = if (isImport) context.getString(R.string.item_title_backup_import_select_file)
                            else context.getString(R.string.item_title_backup_create_select_file),
        iconStatus = if (isImport) R.drawable.img_load else R.drawable.img_save,
    )

    /**
     * Get the loading backup UI state.
     * @param backup the last backup results.
     * @param isImport true for an import, false for an export.
     * @return the loading state.
     */
    private fun getLoadingState(context: Context, backup: Backup.Loading, isImport: Boolean) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.VISIBLE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        iconStatusVisibility = View.GONE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = false,
        textStatusText = if (isImport) context.getString(R.string.message_backup_import_progress, backup.progress ?: 0)
                         else context.getString(R.string.message_backup_create_progress, backup.progress ?: 0, backup.maxProgress ?: 0),
    )

    /** @return Get the verification backup UI state. */
    private fun getVerificationState(context: Context) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.VISIBLE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        iconStatusVisibility = View.GONE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = false,
        textStatusText = context.getString(R.string.message_backup_import_verification)
    )

    /**
     * Get the error UI state.
     * @param isImport true for an import, false for an export.
     * @return the error state.
     */
    private fun getErrorState(context: Context, isImport: Boolean) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.GONE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        iconStatusVisibility = View.VISIBLE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = true,
        textStatusText = if (isImport) context.getString(R.string.message_backup_import_error)
                         else context.getString(R.string.message_backup_create_error),
        iconStatus = R.drawable.img_error,
        iconTint = Color.RED,
    )

    /**
     * Get the completed backup UI state.
     * @param backup the last backup results.
     * @param isImport true for an import, false for an export.
     * @return the completed state.
     */
    private fun getCompletedState(context: Context, backup: Backup.Completed, isImport: Boolean): BackupDialogUiState {
        var iconStatus = R.drawable.img_success
        val textStatus = when {
            !isImport -> context.getString(R.string.message_backup_create_completed)
            backup.failureCount == 0 ->
                context.getString(R.string.message_backup_import_completed, backup.successCount)
            else -> {
                iconStatus = R.drawable.ic_warning
                context.getString(
                    R.string.message_backup_import_completed_with_error,
                    backup.successCount,
                    backup.failureCount
                )
            }
        }

        val compatVisibility = if (backup.compatWarning) {
            iconStatus = R.drawable.ic_warning
            View.VISIBLE
        } else {
            View.GONE
        }

        return BackupDialogUiState(
            fileSelectionVisibility = View.GONE,
            loadingVisibility = View.GONE,
            iconStatusVisibility = View.VISIBLE,
            iconStatus = iconStatus,
            iconTint = if (iconStatus == R.drawable.ic_warning) Color.YELLOW else Color.GREEN,
            textStatusVisibility = View.VISIBLE,
            textStatusText = textStatus,
            compatWarningVisibility = compatVisibility,
            dialogOkButtonEnabled = true,
            dialogCancelButtonEnabled = false,
        )
    }
}

/** Ui state for the backup dialog. */
data class BackupDialogUiState(
    val fileSelectionVisibility: Int,
    val loadingVisibility: Int,
    val textStatusVisibility: Int,
    val compatWarningVisibility: Int,
    val iconStatusVisibility: Int,

    val dialogOkButtonEnabled: Boolean,
    val dialogCancelButtonEnabled: Boolean,

    val fileSelectionText: String? = null,
    val textStatusText: String? = null,
    @DrawableRes val iconStatus: Int? = null,
    @ColorInt val iconTint: Int? = null,
)

/** Zip mime type. */
private const val MIME_TYPE_ZIP = "application/zip"