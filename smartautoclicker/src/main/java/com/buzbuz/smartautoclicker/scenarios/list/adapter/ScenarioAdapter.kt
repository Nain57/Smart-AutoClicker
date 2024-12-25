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
package com.buzbuz.smartautoclicker.scenarios.list.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.databinding.ItemDumbScenarioBinding
import com.buzbuz.smartautoclicker.databinding.ItemEmptyScenarioBinding
import com.buzbuz.smartautoclicker.databinding.ItemSmartScenarioBinding

import kotlinx.coroutines.Job

/**
 * Adapter for the display of the click scenarios created by the user into a RecyclerView.
 *
 * @param startScenarioListener listener upon the click on a scenario.
 * @param exportClickListener listener upon the export button of a scenario.
 * @param deleteScenarioListener listener upon the delete button of a scenario.
 */
class ScenarioAdapter(
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val startScenarioListener: ((ScenarioListUiState.Item) -> Unit),
    private val expandCollapseListener: ((ScenarioListUiState.Item) -> Unit),
    private val exportClickListener: ((ScenarioListUiState.Item) -> Unit),
    private val copyClickedListener: ((ScenarioListUiState.Item.Valid) -> Unit),
    private val deleteScenarioListener: ((ScenarioListUiState.Item) -> Unit),
) : ListAdapter<ScenarioListUiState.Item, RecyclerView.ViewHolder>(ScenarioDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ScenarioListUiState.Item.Empty -> R.layout.item_empty_scenario
            is ScenarioListUiState.Item.Valid.Dumb -> R.layout.item_dumb_scenario
            is ScenarioListUiState.Item.Valid.Smart -> R.layout.item_smart_scenario
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_empty_scenario -> EmptyScenarioHolder(
                viewBinding = ItemEmptyScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                startScenarioListener = startScenarioListener,
                deleteScenarioListener = deleteScenarioListener,
            )

            R.layout.item_dumb_scenario -> DumbScenarioViewHolder(
                viewBinding = ItemDumbScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                startScenarioListener = startScenarioListener,
                expandCollapseListener = expandCollapseListener,
                exportClickListener = exportClickListener,
                copyClickedListener = copyClickedListener,
                deleteScenarioListener = deleteScenarioListener,
            )

            R.layout.item_smart_scenario -> SmartScenarioViewHolder(
                viewBinding = ItemSmartScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                bitmapProvider= bitmapProvider,
                startScenarioListener = startScenarioListener,
                expandCollapseListener = expandCollapseListener,
                exportClickListener = exportClickListener,
                copyClickedListener = copyClickedListener,
                deleteScenarioListener = deleteScenarioListener,
            )

            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyScenarioHolder -> holder.onBind(getItem(position) as ScenarioListUiState.Item.Empty)
            is DumbScenarioViewHolder -> holder.onBind(getItem(position) as ScenarioListUiState.Item.Valid.Dumb)
            is SmartScenarioViewHolder -> holder.onBind(getItem(position) as ScenarioListUiState.Item.Valid.Smart)
        }
    }
}

/** DiffUtil Callback comparing two ScenarioItem when updating the [ScenarioAdapter] list. */
object ScenarioDiffUtilCallback: DiffUtil.ItemCallback<ScenarioListUiState.Item>() {
    override fun areItemsTheSame(oldItem: ScenarioListUiState.Item, newItem: ScenarioListUiState.Item): Boolean =
        when {
            oldItem is ScenarioListUiState.Item.Empty.Dumb && newItem is ScenarioListUiState.Item.Empty.Dumb ->
                oldItem.scenario.id == newItem.scenario.id
            oldItem is ScenarioListUiState.Item.Empty.Smart && newItem is ScenarioListUiState.Item.Empty.Smart ->
                oldItem.scenario.id == newItem.scenario.id
            oldItem is ScenarioListUiState.Item.Valid.Dumb && newItem is ScenarioListUiState.Item.Valid.Dumb ->
                oldItem.scenario.id == newItem.scenario.id
            oldItem is ScenarioListUiState.Item.Valid.Smart && newItem is ScenarioListUiState.Item.Valid.Smart ->
                oldItem.scenario.id == newItem.scenario.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: ScenarioListUiState.Item, newItem: ScenarioListUiState.Item): Boolean =
        oldItem == newItem
}
