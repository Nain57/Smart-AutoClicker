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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.imageevents

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.EventDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.copy.EventCopyDialog
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiImageEvent

import kotlinx.coroutines.launch

class ImageEventListContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: ImageEventListViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageEventListViewModel() },
    )

    /** TouchHelper applied to [eventAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(ImageEventReorderTouchHelper())

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of events. */
    private lateinit var eventAdapter: ImageEventListAdapter

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        eventAdapter = ImageEventListAdapter(
            itemClickedListener = ::onEventItemClicked,
            itemReorderListener = viewModel::updateEventsPriority,
            itemViewBound = ::onEventItemBound,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(
                id = R.string.message_empty_screen_event_title,
                secondaryId = R.string.message_empty_screen_event_desc,
            )
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

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    override fun onCreateButtonClicked() {
        debounceUserInteraction {
            showEventConfigDialog(viewModel.createNewEvent(context))
        }
    }

    override fun onCopyButtonClicked() {
        debounceUserInteraction {
            showEventCopyDialog()
        }
    }

    private fun onEventItemClicked(event: ScreenEvent) {
        debounceUserInteraction {
            showEventConfigDialog(event)
        }
    }

    private fun onEventItemBound(index: Int, eventItemView: View?) {
        if (index != 0) return

        if (eventItemView != null) viewModel.monitorFirstEventView(eventItemView)
        else viewModel.stopViewMonitoring()
    }

    private fun updateEventList(newItems: List<UiImageEvent>?) {
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
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = EventCopyDialog(
                requestTriggerEvents = false,
                onEventSelected = { event -> showEventConfigDialog(viewModel.createNewEvent(context, event as ScreenEvent)) },
            ),
        )
    }

    /** Opens the dialog allowing the user to add a new event. */
    private fun showEventConfigDialog(item: ScreenEvent) {
        viewModel.startEventEdition(item)

        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = EventDialog(
                onConfigComplete = viewModel::saveEventEdition,
                onDelete = viewModel::deleteEditedEvent,
                onDismiss = viewModel::dismissEditedEvent,
            ),
            hideCurrent = true,
        )
    }
}