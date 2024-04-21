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

import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.EventDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.copy.EventCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_ENABLED_ITEM
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

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

    /** Tells if the billing flow has been triggered by the event count limit. */
    private var eventLimitReachedClick: Boolean = false

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        eventAdapter = ImageEventListAdapter(
            itemClickedListener = ::onEventItemClicked,
            itemReorderListener = viewModel::updateEventsPriority,
            itemViewBound = ::onEventItemBound,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(
                id = R.string.message_empty_image_event_list,
                secondaryId = R.string.message_empty_secondary_screen_event_list,
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
        // When the billing flow is not longer displayed, restore the dialogs states
        lifecycleScope.launch {
            repeatOnLifecycle((Lifecycle.State.CREATED)) {
                viewModel.isBillingFlowDisplayed.collect { isDisplayed ->
                    if (!isDisplayed) {
                        if (eventLimitReachedClick) {
                            dialogController.show()
                            eventLimitReachedClick = false
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isEventLimitReached.collect(::updateEventLimitationVisibility) }
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

    private fun onCreateCopyClickedWhileLimited() {
        debounceUserInteraction {
            eventLimitReachedClick = true

            dialogController.hide()
            viewModel.onEventCountReachedAddCopyClicked(context)
        }
    }

    private fun onEventItemClicked(event: ImageEvent) {
        debounceUserInteraction {
            showEventConfigDialog(event)
        }
    }

    private fun onEventItemBound(index: Int, eventItemView: View?) {
        if (index != 0) return

        if (eventItemView != null) viewModel.monitorFirstEventView(eventItemView)
        else viewModel.stopViewMonitoring()
    }

    private fun updateEventLimitationVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.apply {
            if (isVisible) {
                root.alpha = ALPHA_DISABLED_ITEM
                buttonNew.setOnClickListener { onCreateCopyClickedWhileLimited() }
                buttonCopy.setOnClickListener { onCreateCopyClickedWhileLimited() }
            } else {
                root.alpha = ALPHA_ENABLED_ITEM
                buttonNew.setOnClickListener { onCreateButtonClicked() }
                buttonCopy.setOnClickListener { onCopyButtonClicked() }
            }
        }
    }

    private fun updateEventList(newItems: List<ImageEvent>?) {
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
                onEventSelected = { event -> showEventConfigDialog(viewModel.createNewEvent(context, event as ImageEvent)) },
            ),
        )
    }

    /** Opens the dialog allowing the user to add a new event. */
    private fun showEventConfigDialog(item: ImageEvent) {
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