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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText

import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation.CounterCreationDialog

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

class CounterSelectionDialog(
    private val onCounterSelected: (String) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COUNTER_SELECTION.name

    /** The view model for this dialog. */
    private val viewModel: CounterSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterSelectionViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseListBinding

    private lateinit var counterNameAdapter: CounterSelectionAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.generic_counters)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            floatingButtonsLayout.visibility = View.VISIBLE
            buttonCopy.visibility = View.GONE
            buttonNew.visibility = View.VISIBLE
            buttonNew.setDebouncedOnClickListener { showCounterCreationDialog() }

            counterNameAdapter = CounterSelectionAdapter { selectedCounter ->
                debounceUserInteraction {
                    onCounterSelected(selectedCounter.counterName)
                    back()
                }
            }

            layoutLoadableList.apply {
                setEmptyText(R.string.message_empty_counter_name_list_title, R.string.message_empty_counter_name_list_desc)
                list.adapter = counterNameAdapter
                list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.counterNames.collect(::updateCounterNames) }
            }
        }
    }


    private fun updateCounterNames(counterNames: List<CounterSelectionUiItem>) {
        viewBinding.layoutLoadableList.updateState(counterNames)
        counterNameAdapter.submitList(counterNames)
    }

    private fun showCounterCreationDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterCreationDialog(),
            hideCurrent = false,
        )
    }
}
