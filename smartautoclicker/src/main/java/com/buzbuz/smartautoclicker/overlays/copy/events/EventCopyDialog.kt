/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.overlays.copy.events

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.databinding.DialogEventCopyBinding
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListDialog
import com.buzbuz.smartautoclicker.overlays.utils.setIconTint
import kotlinx.coroutines.launch

/**
 *
 */
class EventCopyDialog(
    context: Context,
    scenarioId: Long,
    private val onEventSelected: (Event) -> Unit,
) : LoadableListDialog(context) {

    /** The view model for this dialog. */
    private var viewModel: EventCopyModel? = EventCopyModel(context).apply {
        attachToLifecycle(this@EventCopyDialog)
        setCurrentScenario(scenarioId)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogEventCopyBinding
    /** Adapter displaying the list of events. */
    private lateinit var eventCopyAdapter: EventCopyAdapter

    override val emptyTextId: Int = R.string.dialog_event_copy_empty

    override fun getListBindingRoot(): View = viewBinding.root

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogEventCopyBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(null)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)

        viewBinding.search.apply {
            findViewById<ImageView>(androidx.appcompat.R.id.search_button).setIconTint(R.color.overlayViewPrimary)
            findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn).setIconTint(R.color.overlayViewPrimary)

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel?.updateSearchQuery(newText)
                    return true
                }
            })
        }

        eventCopyAdapter = EventCopyAdapter { selectedEvent ->
            viewModel?.let {
                onEventSelected(it.getCopyEvent(selectedEvent))
                dismiss()
            }
        }

        listBinding.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = eventCopyAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.eventList?.collect { eventList ->
                    updateLayoutState(eventList)
                    eventCopyAdapter.submitList(if (eventList == null) ArrayList() else ArrayList(eventList))
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }
}