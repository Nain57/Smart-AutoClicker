/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.domain.model.action.EventToggle
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.getDividerWithoutHeader
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigEventsToggleBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class EventTogglesDialog(
    private val onConfirmClicked: (List<EventToggle>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: EventTogglesViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventTogglesViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigEventsToggleBinding

    private lateinit var eventToggleAdapter: EventToggleAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigEventsToggleBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_events_toggle)
                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)

                buttonSave.setOnClickListener {
                    debounceUserInteraction {
                        onConfirmClicked(viewModel.getEditedEventToggleList())
                        back()
                    }
                }
                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        back()
                    }
                }
            }

            eventToggleAdapter = EventToggleAdapter(onEventToggleStateChanged = viewModel::changeEventToggleState)

            layoutLoadableList.apply {
                setEmptyText(R.string.message_empty_image_event_list)

                list.apply {
                    addItemDecoration(getDividerWithoutHeader(context))
                    adapter = eventToggleAdapter
                }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentItems.collect(::updateToggleList) }
            }
        }
    }

    private fun updateToggleList(toggleList: List<EventTogglesListItem>) {
        viewBinding.layoutLoadableList.updateState(toggleList)
        eventToggleAdapter.submitList(toggleList)
    }
}