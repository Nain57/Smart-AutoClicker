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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.reference

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

/**
 * Dialog displaying all references to a counter in the scenario.
 *
 * @param counterName The name of the counter to display references for.
 */
class CounterReferenceDialog(
    private val counterName: String,
    private val type: ReferencesType,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COUNTER_REFERENCE.name

    enum class ReferencesType {
        READ,
        WRITE
    }

    /** The view model for this dialog. */
    private val viewModel: CounterReferenceViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterReferenceViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseListBinding
    /** Adapter for the list of references. */
    private lateinit var referenceAdapter: CounterReferenceAdapter

    override fun onCreateView(): ViewGroup {
        viewModel.setDialogArgs(counterName, type)

        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.text = counterName

                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DISMISS, View.VISIBLE)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            floatingButtonsLayout.visibility = View.GONE

            referenceAdapter = CounterReferenceAdapter()
            layoutLoadableList.list.adapter = referenceAdapter
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }
    }

    private fun updateUiState(uiState: List<CounterReferenceUiItem>?) {
        viewBinding.layoutLoadableList.updateState(uiState)
        referenceAdapter.submitList(uiState)
    }
}

