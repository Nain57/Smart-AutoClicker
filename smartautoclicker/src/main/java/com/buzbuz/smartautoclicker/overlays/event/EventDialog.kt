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
package com.buzbuz.smartautoclicker.overlays.event

import android.content.Context
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.DialogButton
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogController
import com.buzbuz.smartautoclicker.overlays.event.actions.ActionsContent
import com.buzbuz.smartautoclicker.overlays.event.conditions.ConditionsContent
import com.buzbuz.smartautoclicker.overlays.event.config.EventConfigContent

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class EventDialog(
    context: Context,
    private val event: Event,
    private val onConfigComplete: (Event) -> Unit,
    private val onDelete: (Event) -> Unit,
): NavBarDialogController(context) {

    /** View model for this dialog. */
    private val viewModel: EventDialogViewModel by lazy { ViewModelProvider(this).get(EventDialogViewModel::class.java) }

    override val navigationMenuId: Int = R.menu.menu_event_config

    override fun onCreateDialog(): BottomSheetDialog {
        viewModel.configuredEvent.value = event

        return super.onCreateDialog().also {
            setButtonVisibility(DialogButton.SAVE, View.VISIBLE)
            if (event.id != 0L) setButtonVisibility(DialogButton.DELETE, View.VISIBLE)

            setTitle(R.string.dialog_event_config_title)
        }
    }

    override fun onCreateContent(navItemId: Int): NavBarDialogContent {
        return when (navItemId) {
            R.id.page_event -> EventConfigContent(viewModel.configuredEvent)
            R.id.page_conditions -> ConditionsContent(viewModel.configuredEvent)
            R.id.page_actions -> ActionsContent(viewModel.configuredEvent)
            else -> throw IllegalArgumentException("Unknown menu id $navItemId")
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isValidEvent.collect(::updateSaveButton) }
            }
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogButton) {
        val event = viewModel.configuredEvent.value ?: return

        when (buttonType) {
            DialogButton.SAVE -> onConfigComplete(event)
            DialogButton.DELETE -> onDelete(event)
            else -> {}
        }

        dismiss()
    }

    private fun updateSaveButton(enabled: Boolean) {
        setButtonEnabledState(DialogButton.SAVE, enabled)
    }
}