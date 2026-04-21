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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.DialogFilteredEventsSelectorBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter.DebugReportTimelineFilter

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue

class FilteredEventsSelectorDialog(
    val eventsFilter: DebugReportTimelineFilter.Events,
    val onFilteredIdsChanged: (DebugReportTimelineFilter.Events) -> Unit,
) : OverlayDialog(R.style.AppTheme) {

    /** View model for this dialog. */
    private val viewModel: FilteredEventsSelectorViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { filteredEventsSelectorViewModel() },
    )

    private lateinit var viewBinding: DialogFilteredEventsSelectorBinding
    private lateinit var adapter: FilteredEventsSelectorAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogFilteredEventsSelectorBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                buttonDelete.visibility = View.GONE

                buttonSave.visibility = View.VISIBLE
                buttonSave.setDebouncedOnClickListener {
                    onFilteredIdsChanged(viewModel.getFilter())
                    back()
                }

                buttonDismiss.visibility = View.VISIBLE
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            adapter = FilteredEventsSelectorAdapter(
                onItemClicked = { id, state ->
                    viewModel.setFilteredState(id, state)
                }
            )
            itemList.adapter = adapter
        }

        viewModel.setEventFilter(eventsFilter)
        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventsItems.collect(::updateItems) }
            }
        }
    }

    private fun updateItems(items: List<FilteredEventsSelectorItem>) {
        adapter.submitList(items)
    }
}