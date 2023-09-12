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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.copy

import android.content.DialogInterface

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.CopyDialog
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.feature.scenario.config.R

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch

class EventCopyDialog(
    private val onEventSelected: (Event) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme) {

    /** View model for this content. */
    private val viewModel: EventCopyModel by lazy { ViewModelProvider(this)[EventCopyModel::class.java] }
    /** Adapter displaying the list of events. */
    private lateinit var eventCopyAdapter: EventCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_event_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        eventCopyAdapter = EventCopyAdapter(::onEventClicked)

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = eventCopyAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventList.collect(::updateEventList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun onEventClicked(event: Event) {
        debounceUserInteraction {
            if (viewModel.eventCopyShouldWarnUser(event)) {
                showToggleEventCopyWarning(event)
            } else {
                notifySelectionAndDestroy(event)
            }
        }
    }

    /** Show the copy event with toggle event action warning. */
    private fun showToggleEventCopyWarning(event: Event) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.message_event_copy_with_toggle_action_from_another_scenario)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                notifySelectionAndDestroy(event)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply {
                window?.setType(DisplayMetrics.TYPE_COMPAT_OVERLAY)
            }
            .show()
    }

    private fun notifySelectionAndDestroy(event: Event) {
        back()
        onEventSelected(event)
    }

    private fun updateEventList(newItems: List<EventCopyModel.EventCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newItems)
        eventCopyAdapter.submitList(newItems)
    }
}