
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.newDividerWithoutHeader
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
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
                dialogTitle.setText(R.string.dialog_title_events_toggle)
                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)

                buttonSave.setDebouncedOnClickListener {
                    onConfirmClicked(viewModel.getEditedEventToggleList())
                    back()
                }
                buttonDismiss.setDebouncedOnClickListener {
                    back()
                }
            }

            eventToggleAdapter = EventToggleAdapter(onEventToggleStateChanged = viewModel::changeEventToggleState)

            layoutLoadableList.apply {
                setEmptyText(R.string.message_empty_screen_event_title)

                list.apply {
                    addItemDecoration(newDividerWithoutHeader(context))
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