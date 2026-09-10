/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.switcher

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemScenarioSwitchBinding

class ScenarioSwitchAdapter(
    private val onScenarioClicked: (Scenario) -> Unit,
) : ListAdapter<Scenario, ScenarioSwitchAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var isSelectionEnabled = true
    private var switchingScenarioId: Identifier? = null

    fun setSelectionEnabled(enabled: Boolean) {
        if (isSelectionEnabled == enabled) return
        isSelectionEnabled = enabled
        notifyDataSetChanged()
    }

    fun setSwitchingScenario(scenarioId: Identifier?) {
        if (switchingScenarioId == scenarioId) return
        switchingScenarioId = scenarioId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemScenarioSwitchBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemScenarioSwitchBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scenario: Scenario) {
            binding.scenarioName.text = scenario.name
            binding.root.isEnabled = isSelectionEnabled
            binding.root.alpha = if (isSelectionEnabled) 1f else 0.6f
            binding.progressSwitching.isVisible = scenario.id == switchingScenarioId
            binding.progressSwitching.contentDescription = binding.root.context.getString(
                R.string.scenario_switcher_switching,
                scenario.name,
            )
            binding.root.contentDescription = scenario.name
            binding.root.setOnClickListener {
                if (isSelectionEnabled) onScenarioClicked(scenario)
            }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Scenario>() {
            override fun areItemsTheSame(oldItem: Scenario, newItem: Scenario): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Scenario, newItem: Scenario): Boolean =
                oldItem == newItem
        }
    }
}
