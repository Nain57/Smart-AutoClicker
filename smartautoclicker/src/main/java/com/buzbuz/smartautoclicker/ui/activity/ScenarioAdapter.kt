/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.ui.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.clicks.database.ScenarioEntity
import com.buzbuz.smartautoclicker.clicks.database.ScenarioWithClicks

import kotlinx.android.synthetic.main.item_scenario.view.btn_delete
import kotlinx.android.synthetic.main.item_scenario.view.details
import kotlinx.android.synthetic.main.item_scenario.view.name

/**
 * Adapter for the display of the click scenarios created by the user into a RecyclerView.
 *
 * @param startClickListener listener upon the click on a scenario.
 * @param deleteClickListener listener upon the delete button of a scenario.
 */
class ScenarioAdapter(
    private val startClickListener: (ScenarioEntity) -> Unit,
    private val deleteClickListener: (ScenarioEntity) -> Unit
) : RecyclerView.Adapter<ScenarioViewHolder>() {

    /** The list of scenarios to be displayed by this adapter. */
    var scenarios: List<ScenarioWithClicks>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = scenarios?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScenarioViewHolder =
        ScenarioViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_scenario, parent, false))

    override fun onBindViewHolder(holder: ScenarioViewHolder, position: Int) {
        val scenarioWithClicks = scenarios!![position]
        holder.itemView.name.text = scenarioWithClicks.scenario.name
        holder.itemView.details.text = holder.itemView.context.resources
            .getQuantityString(R.plurals.scenario_sub_text, scenarioWithClicks.clicks.size, scenarioWithClicks.clicks.size)
        holder.itemView.setOnClickListener { startClickListener(scenarioWithClicks.scenario) }
        holder.itemView.btn_delete.setOnClickListener { deleteClickListener(scenarioWithClicks.scenario) }
    }
}

/** ViewHolder for the [ScenarioAdapter]. */
class ScenarioViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView)