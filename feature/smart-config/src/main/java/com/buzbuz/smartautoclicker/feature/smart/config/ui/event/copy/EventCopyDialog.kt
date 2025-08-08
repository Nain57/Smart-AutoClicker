
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.copy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.newDividerWithoutHeader
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.CopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCopyEventWithToggleEventFromAnotherScenarioDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class EventCopyDialog(
    private val requestTriggerEvents: Boolean,
    private val onEventSelected: (Event) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme) {

    /** View model for this content. */
    private val viewModel: EventCopyModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventCopyModel() },
    )
    /** Adapter displaying the list of events. */
    private lateinit var eventCopyAdapter: EventCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_event_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setCopyListType(requestTriggerEvents)
        eventCopyAdapter = EventCopyAdapter { event ->
            debounceUserInteraction { onEventClicked(event) }
        }

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(newDividerWithoutHeader(context))
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
        if (viewModel.eventCopyShouldWarnUser(event)) {
            context.showCopyEventWithToggleEventFromAnotherScenarioDialog {
                notifySelectionAndDestroy(event)
            }
        } else {
            notifySelectionAndDestroy(event)
        }
    }

    private fun updateEventList(newItems: List<EventCopyModel.EventCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newItems)
        eventCopyAdapter.submitList(newItems)
    }

    private fun notifySelectionAndDestroy(event: Event) {
        back()
        onEventSelected(event)
    }
}