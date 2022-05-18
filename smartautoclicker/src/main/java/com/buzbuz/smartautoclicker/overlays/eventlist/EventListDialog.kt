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
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.databinding.DialogEventListBinding
import com.buzbuz.smartautoclicker.databinding.IncludeEventListButtonsBinding
import com.buzbuz.smartautoclicker.overlays.copy.events.EventCopyDialog
import com.buzbuz.smartautoclicker.overlays.eventconfig.EventConfigDialog
import com.buzbuz.smartautoclicker.overlays.scenariosettings.ScenarioSettingsDialog
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListDialog

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
) : LoadableListDialog(context) {

    /** The view model for this dialog. */
    private var viewModel: EventListModel? = EventListModel(context).apply {
        attachToLifecycle(this@EventListDialog)
        setScenario(scenario)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogEventListBinding
    /** ViewBinding containing the views for the buttons of this dialog. */
    private lateinit var buttonsViewBinding: IncludeEventListButtonsBinding
    /** Adapter displaying the list of events. */
    private lateinit var eventAdapter: EventListAdapter
    /** TouchHelper applied to [eventAdapter] when in [REORDER] mode allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(EventReorderTouchHelper())

    override val emptyTextId: Int = R.string.dialog_event_list_no_events

    override fun getListBindingRoot(): View = viewBinding.layoutList

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogEventListBinding.inflate(LayoutInflater.from(context))
        buttonsViewBinding = viewBinding.includeButtons.apply {
            btnCancel.setOnClickListener { onButtonClicked(it.id) }
            btnConfirm.setOnClickListener { onButtonClicked(it.id) }
            btnCopyEvent.setOnClickListener { onButtonClicked(it.id) }
            btnMoveEvents.setOnClickListener { onButtonClicked(it.id) }
            btnNewEvent.setOnClickListener { onButtonClicked(it.id) }
            btnScenarioSettings.setOnClickListener { onButtonClicked(it.id) }
        }

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_event_list_title)
            .setView(viewBinding.root)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)
        eventAdapter = EventListAdapter(::openEventConfigDialog) { deletedEvent ->
            viewModel?.deleteEvent(deletedEvent)
        }

        listBinding.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = eventAdapter
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
                        eventAdapter.mode = mode
                        when(mode) {
                            EDITION -> toEditionMode()
                            REORDER -> toReorderMode()
                        }
                    }
                }

                launch {
                    viewModel?.copyButtonIsVisible?.collect { isVisible ->
                        if (isVisible) {
                            buttonsViewBinding.btnCopyEvent.visibility = View.VISIBLE
                            buttonsViewBinding.separatorCopyEvent.visibility = View.VISIBLE
                        } else {
                            buttonsViewBinding.btnCopyEvent.visibility = View.GONE
                            buttonsViewBinding.separatorCopyEvent.visibility = View.GONE
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

    private fun onButtonClicked(@IdRes viewId: Int) {
        when (viewId) {
            R.id.btn_new_event -> viewModel?.getNewEvent(context)?.let { openEventConfigDialog(it) }
            R.id.btn_copy_event -> showEventCopyDialog()
            R.id.btn_move_events ->  viewModel?.setUiMode(REORDER)
            R.id.btn_scenario_settings -> showScenarioSettingsDialog()
            R.id.btn_cancel -> {
                eventAdapter.cancelReorder()
                viewModel?.setUiMode(EDITION)
            }
            R.id.btn_confirm -> when (viewModel?.uiMode?.value) {
                EDITION -> dismiss()
                REORDER -> viewModel?.apply {
                    updateEventsPriority((eventAdapter.events!!))
                    setUiMode(EDITION)
                }
            }
        }
    }

    /**
     * Refresh the list of events displayed by the dialog.
     *
     * @param events the new list of events.
     */
    private fun onEventListChanged(events: List<Event>?) {
        updateLayoutState(events)

        eventAdapter.events = events?.toMutableList()

        // In edition, buttons displays depends on the event count, refresh them
        if (viewModel?.uiMode?.value == EDITION) {
            toEditionMode()
        }
    }

    /** Change the Ui mode to [EDITION]. */
    private fun toEditionMode() {
        dialog?.apply {
            itemTouchHelper.attachToRecyclerView(null)

            buttonsViewBinding.apply {
                separatorNewEvent.visibility = View.VISIBLE
                btnNewEvent.visibility = View.VISIBLE
                if (eventAdapter.itemCount > 1) {
                    separatorMoveEvents.visibility = View.VISIBLE
                    btnMoveEvents.visibility = View.VISIBLE
                } else {
                    separatorMoveEvents.visibility = View.GONE
                    btnMoveEvents.visibility = View.GONE
                }

                btnScenarioSettings.visibility = View.VISIBLE
                btnCancel.visibility = View.GONE
            }
        }
    }

    /** Change the Ui mode to [REORDER]. */
    private fun toReorderMode() {
        dialog?.let {
            itemTouchHelper.attachToRecyclerView(listBinding.list)

            buttonsViewBinding.apply {
                separatorNewEvent.visibility = View.GONE
                btnNewEvent.visibility = View.GONE
                separatorMoveEvents.visibility = View.GONE
                btnMoveEvents.visibility = View.GONE
                btnScenarioSettings.visibility = View.GONE
                btnCancel.visibility = View.VISIBLE
            }
        }
    }

    /** Opens the dialog allowing the user to copy a click. */
    private fun showEventCopyDialog() {
        viewModel?.let {
            showSubOverlay(EventCopyDialog(
                context = context,
                scenarioId = it.scenarioId.value!!,
                onEventSelected = ::openEventConfigDialog
            ))
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

    /** Opens the scenario settings dialog. */
    private fun showScenarioSettingsDialog() {
        showSubOverlay(
            overlayController = ScenarioSettingsDialog(
                context = context,
                scenarioId = viewModel?.scenarioId?.value!!
            ),
            hideCurrent = false,
        )
    }
}
