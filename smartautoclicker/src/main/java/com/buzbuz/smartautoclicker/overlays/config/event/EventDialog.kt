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
package com.buzbuz.smartautoclicker.overlays.config.event

import android.content.Context
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogController
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavigationRequest
import com.buzbuz.smartautoclicker.overlays.base.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonVisibility
import com.buzbuz.smartautoclicker.overlays.config.event.actions.ActionsContent
import com.buzbuz.smartautoclicker.overlays.config.event.conditions.ConditionsContent
import com.buzbuz.smartautoclicker.overlays.config.event.config.EventConfigContent

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class EventDialog(
    context: Context,
    private val event: Event,
    private val onConfigComplete: (Event) -> Unit,
    private val onDelete: (Event) -> Unit,
): NavBarDialogController(context) {

    /** View model for this dialog. */
    private val viewModel: EventDialogViewModel by lazy {
        ViewModelProvider(this).get(EventDialogViewModel::class.java)
    }

    override val navigationMenuId: Int = R.menu.menu_event_config

    override fun onCreateView(): ViewGroup {
        viewModel.setConfiguredEvent(event)

        return super.onCreateView().also {
            topBarBinding.apply {
                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                if (event.id != 0L) setButtonVisibility(DialogNavigationButton.DELETE, View.VISIBLE)
                dialogTitle.setText(R.string.dialog_event_config_title)
            }
        }
    }

    override fun onCreateContent(navItemId: Int): NavBarDialogContent {
        return when (navItemId) {
            R.id.page_event -> EventConfigContent()
            R.id.page_conditions -> ConditionsContent()
            R.id.page_actions -> ActionsContent()
            else -> throw IllegalArgumentException("Unknown menu id $navItemId")
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        super.onDialogCreated(dialog)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.navItemsValidity.collect(::updateContentsValidity) }
                launch { viewModel.eventCanBeSaved.collect(::updateSaveButton) }
                launch { viewModel.subOverlayRequest.collect(::onNewSubOverlayRequest) }
            }
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) {
        val event = viewModel.configuredEvent.value ?: return

        when (buttonType) {
            DialogNavigationButton.SAVE -> onConfigComplete(event)
            DialogNavigationButton.DELETE -> onDelete(event)
            else -> {}
        }

        destroy()
    }

    private fun updateContentsValidity(itemsValidity: Map<Int, Boolean>) {
        itemsValidity.forEach { (itemId, isValid) ->
            setMissingInputBadge(itemId, !isValid)
        }
    }

    private fun updateSaveButton(enabled: Boolean) {
        topBarBinding.setButtonEnabledState(DialogNavigationButton.SAVE, enabled)
    }

    private fun onNewSubOverlayRequest(request: NavigationRequest?) {
        if (request == null) return

        showSubOverlay(request.overlay, request.hideCurrent)
        viewModel.consumeRequest()
    }
}