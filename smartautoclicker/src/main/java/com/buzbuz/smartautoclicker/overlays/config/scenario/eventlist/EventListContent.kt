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
package com.buzbuz.smartautoclicker.overlays.config.scenario.eventlist

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavigationRequest
import com.buzbuz.smartautoclicker.overlays.base.bindings.setEmptyText
import com.buzbuz.smartautoclicker.overlays.base.bindings.updateState
import com.buzbuz.smartautoclicker.overlays.config.event.copy.EventCopyDialog
import com.buzbuz.smartautoclicker.overlays.config.event.EventDialog
import com.buzbuz.smartautoclicker.overlays.config.scenario.ConfiguredEvent
import com.buzbuz.smartautoclicker.overlays.config.scenario.ScenarioDialogViewModel

import kotlinx.coroutines.launch

class EventListContent : NavBarDialogContent() {

    /** View model for this content. */
    private val viewModel: EventListViewModel by lazy {
        ViewModelProvider(this).get(EventListViewModel::class.java)
    }
    /** View model for the container dialog. */
    private val dialogViewModel: ScenarioDialogViewModel by lazy {
        ViewModelProvider(dialogController).get(ScenarioDialogViewModel::class.java)
    }

    /** TouchHelper applied to [eventAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(EventReorderTouchHelper())

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of events. */
    private lateinit var eventAdapter: EventListAdapter

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setScenario(dialogViewModel.configuredScenario)

        eventAdapter = EventListAdapter(
            itemClickedListener = ::showEventConfigDialog,
            itemReorderListener = viewModel::updateEventsPriority,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(R.string.message_empty_event_list)
            list.apply {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                itemTouchHelper.attachToRecyclerView(this)
                adapter = eventAdapter
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.copyButtonIsVisible.collect(::updateCopyButtonVisibility) }
                launch { viewModel.eventsItems.collect(::updateEventList) }
            }
        }
    }

    override fun onCreateButtonClicked() {
        showEventConfigDialog(viewModel.getNewEventItem(context))
    }

    override fun onCopyButtonClicked() {
        showEventCopyDialog()
    }

    private fun updateEventList(newItems: List<ConfiguredEvent>?) {
        viewBinding.updateState(newItems)
        eventAdapter.submitList(newItems)
    }

    private fun updateCopyButtonVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.buttonCopy.apply {
            if (isVisible) show() else hide()
        }
    }

    /** Opens the dialog allowing the user to copy an event. */
    private fun showEventCopyDialog() {
        dialogViewModel.requestSubOverlay(
            NavigationRequest(
                EventCopyDialog(
                    context = context,
                    scenarioId = dialogViewModel.configuredScenario.value!!.scenario.id,
                    events = viewModel.getConfiguredEventList(),
                    onEventSelected = { event ->
                        showEventConfigDialog(viewModel.getNewEventItem(context, event))
                    }
                ),
            )
        )
    }

    /** Opens the dialog allowing the user to add a new event. */
    private fun showEventConfigDialog(item: ConfiguredEvent) {
        dialogViewModel.requestSubOverlay(
            NavigationRequest(
                overlay = EventDialog(
                    context = context,
                    event = item.event,
                    onConfigComplete = { viewModel.addOrUpdateEvent(item.copy(event = it)) },
                    onDelete = { viewModel.deleteEvent(item) },
                ),
                hideCurrent = true
            )
        )
    }
}