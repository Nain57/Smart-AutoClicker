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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario

import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialog
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.actionlist.DumbActionListContent
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.config.DumbScenarioConfigContent

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationBarView

import kotlinx.coroutines.launch

class DumbScenarioDialog(
    private val onConfigSaved: () -> Unit,
    private val onConfigDiscarded: () -> Unit,
) : NavBarDialog(R.style.AppTheme) {

    /** View model for this dialog. */
    private val viewModel: DumbScenarioViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbScenarioViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        return super.onCreateView().also {
            topBarBinding.setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
            topBarBinding.dialogTitle.setText(R.string.dialog_overlay_title_dumb_scenario_config)
        }
    }

    override fun inflateMenu(navBarView: NavigationBarView) {
        navBarView.inflateMenu(R.menu.menu_dumb_scenario_config)
    }

    override fun onCreateContent(navItemId: Int): NavBarDialogContent = when (navItemId) {
        R.id.page_actions -> DumbActionListContent(context.applicationContext)
        R.id.page_config -> DumbScenarioConfigContent(context.applicationContext)
        else -> throw IllegalArgumentException("Unknown menu id $navItemId")
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        super.onDialogCreated(dialog)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navItemsValidity.collect(::updateContentsValidity)
                viewModel.canBeSaved.collect(::updateSaveButtonState)
            }
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) {
        if (buttonType == DialogNavigationButton.SAVE) {
            onConfigSaved()
            super.back()
            return
        }

        back()
    }

    override fun back() {
        onConfigDiscarded()
        super.back()
    }

    private fun updateContentsValidity(itemsValidity: Map<Int, Boolean>) {
        itemsValidity.forEach { (itemId, isValid) ->
            setMissingInputBadge(itemId, !isValid)
        }
    }

    private fun updateSaveButtonState(isEnabled: Boolean) {
        topBarBinding.setButtonEnabledState(DialogNavigationButton.SAVE, isEnabled)
    }
}