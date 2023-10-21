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

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.feature.backup.R
import com.buzbuz.smartautoclicker.feature.backup.databinding.DialogBackupBinding

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch

/** Fragment displaying the state of a backup (import or export). */
class BackupDialogFragment : DialogFragment() {

    companion object {

        /** Tag for backup dialog fragment. */
        const val FRAGMENT_TAG_BACKUP_DIALOG = "BackupDialog"
        /** Key for this fragment argument. Tells if the backup is an import (true) or export (false). */
        private const val FRAGMENT_ARG_KEY_IS_IMPORT = ":backup:fragment_args_key_is_import"
        /** Key for this fragment argument. Contains the list of scenario identifier to export (LongArray). */
        private const val FRAGMENT_ARG_KEY_SCENARIO_LIST = ":backup:fragment_args_key_scenario_list"
        /** Key for this fragment argument. Contains the list of dumb scenario identifier to export (LongArray). */
        private const val FRAGMENT_ARG_KEY_DUMB_SCENARIO_LIST = ":backup:fragment_args_key_dumb_scenario_list"

        /**
         * Creates a new instance of this fragment.
         * @param isImport true for an import, false for an export.
         * @param exportSmartScenarios the list of scenario identifier to be exported. Ignored for import.
         * @param exportDumbScenarios the list of dumb scenario identifier to be exported. Ignored for import.
         * @return the new fragment.
         */
        fun newInstance(
            isImport: Boolean,
            exportSmartScenarios: Collection<Long>? = null,
            exportDumbScenarios: Collection<Long>? = null,
        ) : BackupDialogFragment {
            return BackupDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(FRAGMENT_ARG_KEY_IS_IMPORT, isImport)
                    exportSmartScenarios?.let {
                        putLongArray(FRAGMENT_ARG_KEY_SCENARIO_LIST, it.toLongArray())
                    }
                    exportDumbScenarios?.let {
                        putLongArray(FRAGMENT_ARG_KEY_DUMB_SCENARIO_LIST, it.toLongArray())
                    }
                }
            }
        }
    }

    /** The view model containing the backup state. */
    private val backupViewModel: BackupViewModel by viewModels()
    /** The view binding on the views of this dialog.*/
    private lateinit var viewBinding: DialogBackupBinding
    /** The result launcher for the file picker activity. Provides the uri for the backup file. */
    private lateinit var backupActivityResult: ActivityResultLauncher<Intent>

    /** Fragment argument. True for import, false for export. */
    private val isImport: Boolean by lazy { arguments?.getBoolean(FRAGMENT_ARG_KEY_IS_IMPORT)?: false }
    /** Fragment argument, export only. The list of scenario identifier to be exported. */
    private val exportSmartScenarios: List<Long> by lazy {
        arguments?.getLongArray(FRAGMENT_ARG_KEY_SCENARIO_LIST)?.toList() ?: emptyList()
    }
    /** Fragment argument, export only. The list of dumb scenario identifier to be exported. */
    private val exportDumbScenarios: List<Long> by lazy {
        arguments?.getLongArray(FRAGMENT_ARG_KEY_DUMB_SCENARIO_LIST)?.toList() ?: emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backupViewModel.initialize(isImport)

        backupActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    backupViewModel.startBackup(uri, isImport, exportDumbScenarios, exportSmartScenarios)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupViewModel.backupState.collect { backupState ->
                    backupState?.let { onNewState(backupState) }
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogBackupBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isImport) R.string.dialog_title_import_backup else R.string.dialog_title_create_backup)
            .setView(viewBinding.root)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            backupViewModel.backupState.value?.let { onNewState(it) }
        }

        return dialog
    }

    /**
     * Update the UI with the new state.
     * @param state the new UI state.
     */
    private fun onNewState(state: BackupDialogUiState) {
        viewBinding.apply {
            textFileSelection.apply {
                visibility = state.fileSelectionVisibility
                text = state.fileSelectionText

                setOnClickListener {
                    if (!launchDocumentPicker()) {
                        Toast.makeText(context, R.string.message_backup_error_no_zip_app, Toast.LENGTH_LONG).show()
                    }
                }
            }

            loading.visibility = state.loadingVisibility

            textStatus.apply {
                visibility = state.textStatusVisibility
                text = state.textStatusText
            }

            layoutCompatWarning.visibility = state.compatWarningVisibility

            iconStatus.apply {
                visibility = state.iconStatusVisibility
                state.iconStatus?.let { setImageResource(it) }
                state.iconTint?.let { drawable.setTint(it) }
            }

            setDialogButtonsEnabledState(
                enabledPositive = state.dialogOkButtonEnabled,
                enabledNegative = state.dialogCancelButtonEnabled,
            )
        }
    }

    /**
     * Set the enabled state of the dialog buttons.
     * @param enabledPositive true to enable the OK button, false to disable it.
     * @param enabledNegative true to enable the Cancel button, false to disable it.
     */
    private fun setDialogButtonsEnabledState(enabledPositive: Boolean, enabledNegative: Boolean) {
        dialog?.let {
            (it as AlertDialog).apply {
                getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enabledPositive
                getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = enabledNegative
            }
        }
    }

    private fun launchDocumentPicker(): Boolean =
        try {
            backupActivityResult.launch(
                if (isImport) backupViewModel.createBackupRestorationFileSelectionIntent()
                else backupViewModel.createBackupFileCreationIntent()
            )
            true
        } catch (anfex: ActivityNotFoundException) {
            Log.e(TAG, "No application found to load/save a zip file.")
            false
        }
}

private const val TAG = "BackupDialogFragment"