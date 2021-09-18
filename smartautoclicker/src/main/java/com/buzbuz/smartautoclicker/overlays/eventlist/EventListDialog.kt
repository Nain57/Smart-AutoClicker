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
package com.buzbuz.smartautoclicker.overlays.eventlist

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayDialogController
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.database.domain.Scenario
import com.buzbuz.smartautoclicker.databinding.DialogEventListBinding
import com.buzbuz.smartautoclicker.databinding.MergeLoadableListBinding
import com.buzbuz.smartautoclicker.overlays.utils.DialogChoice
import com.buzbuz.smartautoclicker.overlays.utils.MultiChoiceDialog
import com.buzbuz.smartautoclicker.overlays.eventconfig.EventConfigDialog

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * [OverlayDialogController] implementation for displaying a list of events.
 *
 * This dialog allows the user to create/copy/edit/reorder the events for a scenario.
 *
 * @param context the Android Context for the dialog shown by this controller.
 */
class EventListDialog(
    context: Context,
    scenario: Scenario,
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private var viewModel: EventListModel? = EventListModel(context).apply {
        attachToLifecycle(this@EventListDialog)
        setScenario(scenario)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogEventListBinding
    /** ViewBinding containing the views for the loadable list merge layout. */
    private lateinit var listBinding: MergeLoadableListBinding
    /** Adapter displaying the list of events. */
    private lateinit var adapter: EventListAdapter
    /** TouchHelper applied to [adapter] when in [REORDER] mode allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(EventReorderTouchHelper())

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogEventListBinding.inflate(LayoutInflater.from(context))
        listBinding = MergeLoadableListBinding.bind(viewBinding.root)

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_event_list_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.dialog_event_list_reorder, null)
            .setNeutralButton(R.string.dialog_event_list_add, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        adapter = EventListAdapter(::onEventClicked) { deletedEvent ->
            viewModel?.deleteEvent(deletedEvent)
        }

        listBinding.apply {
            list.addItemDecoration(DividerItemDecoration(context,
                DividerItemDecoration.VERTICAL))
            list.adapter = adapter
            empty.setText(R.string.dialog_event_list_no_events)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel?.events?.collect {
                        onEventListChanged(it)
                    }
                }

                launch {
                    viewModel?.uiMode?.collect { mode ->
                        adapter.mode = mode
                        when(mode) {
                            EDITION -> toEditionMode()
                            COPY -> toCopyMode()
                            REORDER -> toReorderMode()
                        }
                    }
                }
            }
        }
    }

    override fun onVisibilityChanged(visible: Boolean) {
        if (visible) viewModel?.setUiMode(EDITION)
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }

    /**
     * Refresh the list of events displayed by the dialog.
     *
     * @param events the new list of events.
     */
    private fun onEventListChanged(events: List<Event>?) {
        listBinding.apply {
            loading.visibility = View.GONE
            if (events.isNullOrEmpty()) {
                list.visibility = View.GONE
                empty.visibility = View.VISIBLE
            } else {
                list.visibility = View.VISIBLE
                empty.visibility = View.GONE
            }
        }

        adapter.clicks = events?.toMutableList()

        // In edition, buttons displays depends on the event count, refresh them
        if (viewModel?.uiMode?.value == EDITION) {
            toEditionMode()
        }
    }

    /** Change the Ui mode to [EDITION]. */
    private fun toEditionMode() {
        dialog?.apply {
            itemTouchHelper.attachToRecyclerView(null)

            changeButtonState(
                button = getButton(AlertDialog.BUTTON_POSITIVE),
                visibility = View.VISIBLE,
                textId = android.R.string.ok,
                listener = { dismiss() },
            )
            changeButtonState(
                button = getButton(AlertDialog.BUTTON_NEGATIVE),
                visibility = if (adapter.itemCount > 1) View.VISIBLE else View.INVISIBLE,
                textId = R.string.dialog_event_list_reorder,
                listener = { viewModel?.setUiMode(REORDER) },
            )
            changeButtonState(getButton(AlertDialog.BUTTON_NEUTRAL), View.VISIBLE, R.string.dialog_event_list_add) {
                onAddClicked()
            }
        }
    }

    /** Change the Ui mode to [COPY]. */
    private fun toCopyMode() {
        dialog?.apply {
            itemTouchHelper.attachToRecyclerView(null)
            changeButtonState(
                button = getButton(DialogInterface.BUTTON_POSITIVE),
                visibility = View.VISIBLE,
                textId =  android.R.string.cancel,
                listener = { viewModel?.setUiMode(EDITION) }
            )
            changeButtonState(getButton(DialogInterface.BUTTON_NEUTRAL), View.GONE)
            changeButtonState(getButton(DialogInterface.BUTTON_NEGATIVE), View.GONE)
        }
    }

    /** Change the Ui mode to [REORDER]. */
    private fun toReorderMode() {
        dialog?.let {
            itemTouchHelper.attachToRecyclerView(listBinding.list)
            changeButtonState(it.getButton(AlertDialog.BUTTON_POSITIVE), View.VISIBLE, android.R.string.ok) {
                viewModel?.updateEventsPriority((adapter.clicks!!))
                viewModel?.setUiMode(EDITION)
            }
            changeButtonState(it.getButton(AlertDialog.BUTTON_NEGATIVE), View.VISIBLE, android.R.string.cancel) {
                adapter.cancelReorder()
                viewModel?.setUiMode(EDITION)
            }
            changeButtonState(it.getButton(AlertDialog.BUTTON_NEUTRAL), View.GONE)
        }
    }

    /**
     * Called when the user clicks on the "Add" button.
     * According to the current click count for the current scenario, it will directly starts the click configuration
     * dialog to create a new click, or it will display the New/Copy dialog first.
     */
    private fun onAddClicked() {
        if (adapter.itemCount > 0) {
            showSubOverlay(MultiChoiceDialog(
                context = context,
                dialogTitle = R.string.dialog_event_add_title,
                choices = listOf(CreateEventChoice.Create, CreateEventChoice.Copy),
                onChoiceSelected = { choiceClicked ->
                    when (choiceClicked) {
                        is CreateEventChoice.Create -> viewModel?.getNewEvent(context)?.let {
                            openEventConfigDialog(it)
                        }
                        is CreateEventChoice.Copy -> viewModel?.setUiMode(COPY)
                    }
                }
            ))
        } else {
            viewModel?.getNewEvent(context)?.let {
                openEventConfigDialog(it)
            }
        }
    }

    /**
     * Handle the click on an event.
     * @param event the event clicked.
     */
    private fun onEventClicked(event: Event) {
        viewModel?.getCompleteEvent(event, viewModel!!.uiMode.value == COPY) { completeEvent->
            openEventConfigDialog(completeEvent)
        }
    }

    /** Opens the dialog allowing the user to add a new click. */
    private fun openEventConfigDialog(event: Event) {
        showSubOverlay(
            overlayController = EventConfigDialog(
                context = context,
                event = event,
                onConfigComplete = { configuredEvent ->
                    viewModel?.addOrUpdateEvent(configuredEvent)
                }),
            true
        )
    }
}

/**
 * Dialog choice for the event creation.
 *
 * @param title the string res of the title for this choice.
 * @param iconId the icon res of the image for this choice. Can be null if no image are requested.
 */
sealed class CreateEventChoice(title: Int, iconId: Int?): DialogChoice(title, iconId) {
    /** Choice for creating a new Event. */
    object Create : CreateEventChoice(R.string.dialog_event_add_create, R.drawable.ic_add)
    /** Choice for copying an Event. */
    object Copy : CreateEventChoice(R.string.dialog_event_add_copy, R.drawable.ic_copy)
}