/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.activity

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.activity.ScenarioListFragmentUiState.ScenarioListItem
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.databinding.ItemScenarioBinding
import com.buzbuz.smartautoclicker.databinding.ItemScenarioEmptyBinding
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition

import kotlinx.coroutines.Job

/**
 * Adapter for the display of the click scenarios created by the user into a RecyclerView.
 *
 * @param startScenarioListener listener upon the click on a scenario.
 * @param exportClickListener listener upon the export button of a scenario.
 * @param deleteScenarioListener listener upon the delete button of a scenario.
 */
class ScenarioAdapter(
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val startScenarioListener: ((Scenario) -> Unit),
    private val exportClickListener: ((Scenario) -> Unit),
    private val deleteScenarioListener: ((Scenario) -> Unit),
) : ListAdapter<ScenarioListItem, RecyclerView.ViewHolder>(ScenarioDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ScenarioListItem.EmptyScenarioItem -> R.layout.item_scenario_empty
            is ScenarioListItem.ScenarioItem -> R.layout.item_scenario
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_scenario_empty -> EmptyScenarioHolder(
                ItemScenarioEmptyBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                startScenarioListener,
                deleteScenarioListener,
            )

            R.layout.item_scenario -> ScenarioViewHolder(
                ItemScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                bitmapProvider,
                startScenarioListener,
                exportClickListener,
                deleteScenarioListener,
            )

            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyScenarioHolder -> holder.onBind(getItem(position) as ScenarioListItem.EmptyScenarioItem)
            is ScenarioViewHolder -> holder.onBind(getItem(position) as ScenarioListItem.ScenarioItem)
        }
    }
}

/** DiffUtil Callback comparing two ScenarioItem when updating the [ScenarioAdapter] list. */
object ScenarioDiffUtilCallback: DiffUtil.ItemCallback<ScenarioListItem>() {
    override fun areItemsTheSame(oldItem: ScenarioListItem, newItem: ScenarioListItem): Boolean =
        when {
            oldItem is ScenarioListItem.EmptyScenarioItem && newItem is ScenarioListItem.EmptyScenarioItem ->
                oldItem.scenario.id == newItem.scenario.id
            oldItem is ScenarioListItem.ScenarioItem && newItem is ScenarioListItem.ScenarioItem ->
                oldItem.scenario.id == newItem.scenario.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: ScenarioListItem, newItem: ScenarioListItem): Boolean = oldItem == newItem
}

class EmptyScenarioHolder(
    private val viewBinding: ItemScenarioEmptyBinding,
    private val startScenarioListener: ((Scenario) -> Unit),
    private val deleteScenarioListener: ((Scenario) -> Unit),
): RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(scenarioItem: ScenarioListItem.EmptyScenarioItem) = viewBinding.apply {
        scenarioName.text = scenarioItem.scenario.name

        buttonStart.setOnClickListener { startScenarioListener(scenarioItem.scenario) }
        buttonDelete.setOnClickListener { deleteScenarioListener(scenarioItem.scenario) }
    }
}

/** ViewHolder for the [ScenarioAdapter]. */
class ScenarioViewHolder(
    private val viewBinding: ItemScenarioBinding,
    bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val startScenarioListener: ((Scenario) -> Unit),
    private val exportClickListener: ((Scenario) -> Unit),
    private val deleteScenarioListener: ((Scenario) -> Unit),
) : RecyclerView.ViewHolder(viewBinding.root) {

    private val eventsAdapter = ScenarioEventsAdapter(bitmapProvider)

    init {
        viewBinding.listEvent.adapter = eventsAdapter
    }

    fun onBind(scenarioItem: ScenarioListItem.ScenarioItem) = viewBinding.apply {
        scenarioName.text = scenarioItem.scenario.name
        eventsAdapter.submitList(scenarioItem.eventsItems)

        if (scenarioItem.exportMode) {
            buttonDelete.visibility = View.GONE
            buttonStart.visibility = View.GONE
            buttonExport.apply {
                visibility = View.VISIBLE
                isChecked = scenarioItem.checkedForExport
            }
        } else {
            buttonDelete.visibility = View.VISIBLE
            buttonStart.visibility = View.VISIBLE
            buttonExport.visibility = View.GONE
        }

        buttonStart.setOnClickListener { startScenarioListener(scenarioItem.scenario) }
        buttonDelete.setOnClickListener { deleteScenarioListener(scenarioItem.scenario) }
        buttonExport.setOnClickListener { exportClickListener(scenarioItem.scenario) }
    }
}