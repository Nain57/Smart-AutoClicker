/*
 * Copyright (C) 2021 Nain57
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

import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.domain.Scenario
import com.buzbuz.smartautoclicker.databinding.ItemScenarioBinding

/** Adapter for the display of the click scenarios created by the user into a RecyclerView. */
class ScenarioAdapter : RecyclerView.Adapter<ScenarioViewHolder>() {

    /** The list of scenarios to be displayed by this adapter. */
    var scenarios: List<Scenario>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    /** Listener upon the click on a scenario. */
    var startScenarioListener: ((Scenario) -> Unit)? = null
    /** Listener upon the rename button of a scenario. */
    var editClickListener: ((Scenario) -> Unit)? = null
    /** Listener upon the delete button of a scenario. */
    var deleteScenarioListener: ((Scenario) -> Unit)? = null

    override fun getItemCount(): Int = scenarios?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScenarioViewHolder =
        ScenarioViewHolder(ItemScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ScenarioViewHolder, position: Int) {
        val scenario = scenarios!![position]
        holder.viewBinding.name.text = scenario.name
        holder.viewBinding.details.text = holder.itemView.context.resources
            .getQuantityString(R.plurals.scenario_sub_text, scenario.eventCount, scenario.eventCount)
        setClickListener(holder.viewBinding.root, scenario, startScenarioListener)
        setClickListener(holder.viewBinding.btnDelete, scenario, deleteScenarioListener)
        setClickListener(holder.viewBinding.btnRename, scenario, editClickListener)
    }

    /**
     * Set the provided listener to a view, if any. If none is provided, attach null.
     *
     * @param view the view to attach the listener to.
     * @param scenario the scenario used as argument for the listener lambda
     * @param listener the listener to notify upon click.
     */
    private fun setClickListener(view: View, scenario: Scenario, listener: ((Scenario) -> Unit)?) =
        listener?.let {
            view.setOnClickListener { it(scenario) }
        } ?: view.setOnClickListener(null)
}

/** ViewHolder for the [ScenarioAdapter]. */
class ScenarioViewHolder(val viewBinding: ItemScenarioBinding) : RecyclerView.ViewHolder(viewBinding.root)