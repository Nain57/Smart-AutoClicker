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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.counter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText

import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class CounterNameSelectionDialog(
    private val onCounterSelected: (String) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: CounterNameSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterNameSelectionViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    private lateinit var counterNameAdapter: CounterNameSelectionAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_counter_name_selection)
                buttonDismiss.setOnClickListener { debounceUserInteraction { back() } }
            }

            counterNameAdapter = CounterNameSelectionAdapter { selectedCounterName ->
                debounceUserInteraction {
                    onCounterSelected(selectedCounterName)
                    back()
                }
            }

            layoutLoadableList.apply {
                setEmptyText(R.string.message_empty_counter_name_list, R.string.message_empty_secondary_counter_name_list)
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


    private fun updateCounterNames(counterNames: Set<String>) {
        viewBinding.layoutLoadableList.updateState(counterNames)
        counterNameAdapter.submitList(counterNames.toList())
    }
}