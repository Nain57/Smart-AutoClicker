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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.switcher

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemCounterNameBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ScenarioSwitchDialog(
    private val onScenarioSelected: (Scenario) -> Boolean,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ScenarioSwitchViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { scenarioSwitchViewModel() },
    )

    private lateinit var viewBinding: DialogBaseSelectionBinding

    private val scenarioAdapter = ScenarioSwitchAdapter { scenario ->
        debounceUserInteraction {
            if (onScenarioSelected(scenario)) back()
        }
    }

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_switch_scenario)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            layoutLoadableList.apply {
                setEmptyText(R.string.message_empty_scenario_switch_list_title)
                list.adapter = scenarioAdapter
                list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.switchableScenarios.collect(::updateScenarios) }
            }
        }
    }

    private fun updateScenarios(scenarios: List<Scenario>) {
        viewBinding.layoutLoadableList.updateState(scenarios)
        scenarioAdapter.submitList(scenarios)
    }
}

private class ScenarioSwitchAdapter(
    private val onScenarioSelected: (Scenario) -> Unit,
) : ListAdapter<Scenario, ScenarioSwitchViewHolder>(ScenarioDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScenarioSwitchViewHolder =
        ScenarioSwitchViewHolder(
            viewBinding = ItemCounterNameBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onScenarioSelected = onScenarioSelected,
        )

    override fun onBindViewHolder(holder: ScenarioSwitchViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

private object ScenarioDiffUtilCallback: DiffUtil.ItemCallback<Scenario>() {
    override fun areItemsTheSame(oldItem: Scenario, newItem: Scenario): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Scenario, newItem: Scenario): Boolean =
        oldItem == newItem
}

private class ScenarioSwitchViewHolder(
    private val viewBinding: ItemCounterNameBinding,
    private val onScenarioSelected: (Scenario) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(scenario: Scenario) {
        viewBinding.textCounterName.text = scenario.name
        viewBinding.root.setOnClickListener { onScenarioSelected(scenario) }
    }
}
