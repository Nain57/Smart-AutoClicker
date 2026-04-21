/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setEnabled
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.DialogDebugReportTimelineFiltersBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter.events.FilteredEventsSelectorDialog

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue


/** Displays the filters applied to the debugging timeline. */
class DebugReportTimelineFiltersDialog(
    private val reportDurationMs: Long,
    private val currentFilters: List<DebugReportTimelineFilter>,
    private val onFiltersApplied: (List<DebugReportTimelineFilter>) -> Unit,
): OverlayDialog(R.style.AppTheme) {

    /** View model for this dialog. */
    private val viewModel: DebugReportTimelineFiltersViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportTimelineFiltersViewModel() },
    )

    private lateinit var viewBinding: DialogDebugReportTimelineFiltersBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogDebugReportTimelineFiltersBinding.inflate(LayoutInflater.from(context)).apply {
            // Top bar
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_timeline_filters)

                buttonDelete.visibility = View.GONE
                buttonDismiss.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { back() }
                }

                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSave() }
                }
            }

            // Time filter
            rangeSliderTime.addOnChangeListener { rangeSlider, value, fromUser ->
                if (!fromUser) return@addOnChangeListener

                if (value == rangeSlider.values[0]) viewModel.setUserTimeLowerBound(value.toLong())
                else if (value == rangeSlider.values[1]) viewModel.setUserTimeUpperBound(value.toLong())
            }

            // Image events filter
            fieldShowImageEvents.apply {
                setTitle(context.getString(R.string.field_debug_filter_image_events_title))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_debug_filter_image_events_show_desc_on),
                        context.getString(R.string.field_debug_filter_image_events_show_desc_off),
                    )
                )
                setOnClickListener { viewModel.toggleShowImageEvents() }
            }
            fieldSelectImageEvents.apply {
                setTitle(context.getString(R.string.field_debug_filter_events_show_title))
                setOnClickListener {
                    showEventSelectionDialog(viewModel.getImageEventsFilter())
                }
            }

            // Trigger events filter
            fieldShowTriggerEvents.apply {
                setTitle(context.getString(R.string.field_debug_filter_trigger_events_title))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_debug_filter_trigger_events_show_desc_on),
                        context.getString(R.string.field_debug_filter_trigger_events_show_desc_off),
                    )
                )
                setOnClickListener { viewModel.toggleShowTriggerEvents() }
            }
            fieldSelectTriggerEvents.apply {
                setTitle(context.getString(R.string.field_debug_filter_events_show_title))
                setOnClickListener {
                    showEventSelectionDialog(viewModel.getTriggerEventsFilter())
                }
            }
        }

        viewModel.setupUserValues(context, reportDurationMs, currentFilters)
        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.timeUiState.collect(::updateTimeFilterUiState) }
                launch { viewModel.imageEventsUiState.collect(::updateImageEventFilterUiState) }
                launch { viewModel.triggerEventsUiState.collect(::updateTriggerEventFilterUiState) }
            }
        }
    }

    private fun onSave() {
        onFiltersApplied(viewModel.getAllFilters())
        back()
    }

    private fun updateTimeFilterUiState(uiState: DebugReportTimeFilterUiState?) {
        uiState ?: return

        viewBinding.apply {
            textStartTime.text = uiState.lowerValueText
            textEndTime.text = uiState.upperValueText
            rangeSliderTime.apply {
                valueFrom = uiState.lowerBoundMs.toFloat()
                valueTo = uiState.upperBoundMs.toFloat()
                values = listOf(
                    uiState.lowerValueMs.toFloat(),
                    uiState.upperValueMs.toFloat(),
                )
            }
        }
    }

    private fun updateImageEventFilterUiState(uiState: DebugReportImageEventFilterUiState?) {
        uiState ?: return

        viewBinding.fieldShowImageEvents.apply {
            setChecked(uiState.checkboxState)
            setDescription(uiState.checkboxDescId)
        }

        viewBinding.fieldSelectImageEvents.apply {
            setEnabled(uiState.filteredIdsSelectorState)
            setDescription(uiState.filteredIdsText)
        }
    }

    private fun updateTriggerEventFilterUiState(uiState: DebugReportTriggerEventFilterUiState?) {
        uiState ?: return

        viewBinding.fieldShowTriggerEvents.apply {
            setChecked(uiState.checkboxState)
            setDescription(uiState.checkboxDescId)
        }

        viewBinding.fieldSelectTriggerEvents.apply {
            setEnabled(uiState.filteredIdsSelectorState)
            setDescription(uiState.filteredIdsText)
        }
    }

    private fun showEventSelectionDialog(filter: DebugReportTimelineFilter.Events) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = FilteredEventsSelectorDialog(
                eventsFilter = filter,
                onFilteredIdsChanged = { viewModel.setEventsFilter(context, filter = it) }
            ),
            hideCurrent = false,
        )
    }
}