/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.action.intent

import android.content.Context
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.baseui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.baseui.bindings.setButtonVisibility
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogController

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class IntentDialog(
    context: Context,
    private val editedIntent: EditedAction,
    private val onDeleteClicked: (EditedAction) -> Unit,
    private val onConfirmClicked: (EditedAction) -> Unit,
) : NavBarDialogController(context) {

    /** The view model for this dialog. */
    private val viewModel: IntentViewModel by lazy {
        ViewModelProvider(this).get(IntentViewModel::class.java)
    }

    override val navigationMenuId: Int = R.menu.menu_intent_config

    override fun onCreateView(): ViewGroup {
        viewModel.setConfiguredIntent(editedIntent)

        return super.onCreateView().also {
            topBarBinding.dialogTitle.setText(R.string.dialog_overlay_title_intent)
            topBarBinding.setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        navBarView.selectedItemId =
            if (viewModel.isAdvanced()) R.id.page_advanced
            else R.id.page_simple

        super.onDialogCreated(dialog)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    override fun onCreateContent(navItemId: Int): NavBarDialogContent {
        return when (navItemId) {
            R.id.page_simple -> SimpleIntentContent()
            R.id.page_advanced -> AdvancedIntentContent()
            else -> throw IllegalArgumentException("Unknown menu id $navItemId")
        }
    }

    override fun onContentViewChanged(navItemId: Int) {
        when (navItemId) {
            R.id.page_simple -> viewModel.setIsAdvancedConfiguration(false)
            R.id.page_advanced -> viewModel.setIsAdvancedConfiguration(true)
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) {
        when (buttonType) {
            DialogNavigationButton.SAVE -> {
                viewModel.saveLastConfig()
                onConfirmClicked(viewModel.getConfiguredIntent())
            }
            DialogNavigationButton.DELETE -> onDeleteClicked(editedIntent)
            else -> {}
        }

        destroy()
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        topBarBinding.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }
}