/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.more.debugview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogDebugViewSettingsBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch


class DebugViewSettingsDialog : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: DebugViewSettingsViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { debugViewSettingsViewModel() },
    )

    private lateinit var viewBinding: DialogDebugViewSettingsBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogDebugViewSettingsBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_debug_view_settings)

                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DISMISS, View.VISIBLE)
                buttonDismiss.setIconResource(R.drawable.ic_back)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            fieldShowEventStats.apply {
                setTitle(context.getString(R.string.field_debug_view_show_event_stats_title))
                setOnClickListener(viewModel::toggleShowEventStats)
            }

            fieldShowActions.apply {
                setTitle(context.getString(R.string.field_debug_view_show_actions_title))
                setOnClickListener(viewModel::toggleShowActions)
            }

            fieldShowHeldFinger.apply {
                setTitle(context.getString(R.string.field_debug_view_show_held_finger_title))
                setOnClickListener(viewModel::toggleShowHeldFinger)
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }
    }

    private fun updateUiState(state: DebugViewSettingsUiState) {
        viewBinding.apply {
            fieldShowEventStats.setChecked(state.showEventStats)
            fieldShowActions.setChecked(state.showActions)
            fieldShowHeldFinger.setChecked(state.showHeldFinger)
        }
    }
}
