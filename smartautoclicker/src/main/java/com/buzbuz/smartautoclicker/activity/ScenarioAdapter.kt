/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.databinding.ItemScenarioBinding

/**
 * Adapter for the display of the click scenarios created by the user into a RecyclerView.
 *
 * @param startScenarioListener listener upon the click on a scenario.
 * @param editClickListener listener upon the rename button of a scenario.
 * @param exportClickListener listener upon the export button of a scenario.
 * @param deleteScenarioListener listener upon the delete button of a scenario.
 */
class ScenarioAdapter(
    private val startScenarioListener: ((Scenario) -> Unit),
    private val editClickListener: ((Scenario) -> Unit),
    private val exportClickListener: ((Scenario) -> Unit),
    private val deleteScenarioListener: ((Scenario) -> Unit),
) : ListAdapter<ScenarioItem, ScenarioViewHolder>(ScenarioDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScenarioViewHolder =
        ScenarioViewHolder(
            ItemScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            startScenarioListener,
            editClickListener,
            exportClickListener,
            deleteScenarioListener,
        )

    override fun onBindViewHolder(holder: ScenarioViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

/** DiffUtil Callback comparing two ScenarioItem when updating the [ScenarioAdapter] list. */
object ScenarioDiffUtilCallback: DiffUtil.ItemCallback<ScenarioItem>() {
    override fun areItemsTheSame(oldItem: ScenarioItem, newItem: ScenarioItem): Boolean =
        oldItem.scenario.id == newItem.scenario.id

    override fun areContentsTheSame(oldItem: ScenarioItem, newItem: ScenarioItem): Boolean = oldItem == newItem
}

/** ViewHolder for the [ScenarioAdapter]. */
class ScenarioViewHolder(
    private val viewBinding: ItemScenarioBinding,
    private val startScenarioListener: ((Scenario) -> Unit),
    private val editClickListener: ((Scenario) -> Unit),
    private val exportClickListener: ((Scenario) -> Unit),
    private val deleteScenarioListener: ((Scenario) -> Unit),
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(scenarioItem: ScenarioItem) = viewBinding.apply {
        name.text = scenarioItem.scenario.name
        details.text = itemView.context.resources.getQuantityString(
            R.plurals.scenario_sub_text,
            scenarioItem.scenario.eventCount,
            scenarioItem.scenario.eventCount,
        )

        if (scenarioItem.exportMode) {
            btnDelete.visibility = View.GONE
            btnRename.visibility = View.GONE
            btnExport.apply {
                visibility = View.VISIBLE
                isChecked = scenarioItem.checkedForExport
            }
        } else {
            btnDelete.visibility = View.VISIBLE
            btnRename.visibility = View.VISIBLE
            btnExport.visibility = View.GONE
        }

        root.setOnClickListener { startScenarioListener(scenarioItem.scenario) }
        btnDelete.setOnClickListener { deleteScenarioListener(scenarioItem.scenario) }
        btnRename.setOnClickListener { editClickListener(scenarioItem.scenario) }
        btnExport.setOnClickListener { exportClickListener(scenarioItem.scenario) }
    }
}