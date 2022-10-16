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
package com.buzbuz.smartautoclicker.overlays.scenario

import android.content.Context
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogController
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.NavigationRequest
import com.buzbuz.smartautoclicker.overlays.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.overlays.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.overlays.bindings.setButtonVisibility
import com.buzbuz.smartautoclicker.overlays.scenario.config.ScenarioConfigContent
import com.buzbuz.smartautoclicker.overlays.scenario.debug.DebugConfigContent
import com.buzbuz.smartautoclicker.overlays.scenario.eventlist.EventListContent

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ScenarioDialog(
    context: Context,
    private val scenario: Scenario,
) : NavBarDialogController(context) {

    /** The view model for this dialog. */
    private val viewModel: ScenarioDialogViewModel by lazy { ViewModelProvider(this).get(ScenarioDialogViewModel::class.java) }

    override val navigationMenuId: Int = R.menu.menu_scenario_config

    override fun onCreateDialog(): BottomSheetDialog {
        viewModel.setConfiguredScenario(scenario.id)
        return super.onCreateDialog().also {
            topBarBinding.setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
        }
    }

    override fun onCreateContent(navItemId: Int): NavBarDialogContent = when (navItemId) {
        R.id.page_events -> EventListContent(scenario.id)
        R.id.page_config -> ScenarioConfigContent(scenario.id)
        R.id.page_debug -> DebugConfigContent()
        else -> throw IllegalArgumentException("Unknown menu id $navItemId")
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.saveEnabledState.collect(::updateSaveButtonState) }
                launch { viewModel.scenarioName.collect(::updateDialogTitle) }
                launch { viewModel.subOverlayRequest.collect(::onNewSubOverlayRequest) }
            }
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) = dismiss()

    /** */
    private fun updateSaveButtonState(isEnabled: Boolean) {
        topBarBinding.setButtonEnabledState(DialogNavigationButton.SAVE, isEnabled)
    }

    /** */
    private fun updateDialogTitle(scenarioName: String) {
        topBarBinding.dialogTitle.text = scenarioName
    }

    private fun onNewSubOverlayRequest(request: NavigationRequest?) {
        if (request == null) return

        showSubOverlay(request.overlay, request.hideCurrent)
        viewModel.consumeRequest()
    }
}