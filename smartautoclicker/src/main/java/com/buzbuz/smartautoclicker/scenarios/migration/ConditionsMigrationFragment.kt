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
package com.buzbuz.smartautoclicker.scenarios.migration

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setState

import com.buzbuz.smartautoclicker.databinding.DialogConditionsMigrationBinding

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConditionsMigrationFragment : DialogFragment() {

    companion object {

        /** Tag for backup dialog fragment. */
        const val FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG = "ConditionsMigrationDialog"
        /** Tells if the migration is completed. */
        const val FRAGMENT_RESULT_KEY_COMPLETED = ":$FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG:state"

        /** Creates a new instance of this fragment. */
        fun newInstance() : ConditionsMigrationFragment {
            return ConditionsMigrationFragment()
        }
    }

    /** ViewModel providing the click scenarios data to the UI. */
    private val viewModel: ConditionsMigrationViewModel by viewModels()
    /** ViewBinding for this dialog. */
    private lateinit var viewBinding: DialogConditionsMigrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogConditionsMigrationBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireContext())
            .setView(viewBinding.root)
            .create()
    }

    private fun updateUiState(conditionsMigrationUiState: ConditionsMigrationUiState) {
        viewBinding.textState.text = conditionsMigrationUiState.textState
        viewBinding.buttonMigration.apply {
            setState(conditionsMigrationUiState.buttonState)
            setOnClickListener {
                when (conditionsMigrationUiState.migrationState) {
                    MigrationState.NOT_STARTED -> viewModel.startMigration()
                    MigrationState.STARTED -> Unit
                    MigrationState.FINISHED,
                    MigrationState.FINISHED_WITH_ERROR -> {
                        setFragmentResult(FRAGMENT_RESULT_KEY_COMPLETED, Bundle.EMPTY)
                        dismiss()
                    }
                }
            }
        }
    }
}