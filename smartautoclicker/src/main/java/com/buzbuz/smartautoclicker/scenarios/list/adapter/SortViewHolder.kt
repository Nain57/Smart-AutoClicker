/*
 * Copyright (C) 2025 Kevin Buzeau
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

import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemOrderingAndFilteringBinding
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortType
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener

class SortViewHolder(
    private val viewBinding: ItemOrderingAndFilteringBinding,
    private val onSortTypeClicked: (ScenarioSortType) -> Unit,
    private val onSmartChipClicked: (Boolean) -> Unit,
    private val onDumbChipClicked: (Boolean) -> Unit,
    private val onSortOrderClicked: (Boolean) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    private val onOrderingButtonGroupCheckedListener =
        OnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) onSortTypeClicked(checkedId.toScenarioSortType())
        }

    fun onBind(config: ScenarioListUiState.Item.SortItem) {
        viewBinding.apply {
            buttonGroupOrdering.apply {
                check(config.sortType.toButtonId())
                addOnButtonCheckedListener(onOrderingButtonGroupCheckedListener)
            }

            chipSmart.apply {
                isChecked = config.smartVisible
                setOnClickListener { onSmartChipClicked(chipSmart.isChecked) }
            }
            chipDumb.apply {
                isChecked = config.dumbVisible
                setOnClickListener { onDumbChipClicked(chipDumb.isChecked) }
            }
            checkboxSortOrder.apply {
                isChecked = config.changeOrderChecked
                setOnClickListener { onSortOrderClicked(checkboxSortOrder.isChecked) }
            }
        }
    }

    fun onUnbind() {
        viewBinding.apply {
            buttonGroupOrdering.removeOnButtonCheckedListener(onOrderingButtonGroupCheckedListener)
            chipSmart.setOnClickListener(null)
            chipDumb.setOnClickListener(null)
            checkboxSortOrder.setOnClickListener(null)
        }
    }
}

@IdRes
private fun ScenarioSortType.toButtonId(): Int =
    when (this) {
        ScenarioSortType.NAME -> R.id.button_name
        ScenarioSortType.RECENT -> R.id.button_recent
        ScenarioSortType.MOST_USED -> R.id.button_most_used
    }

private fun Int.toScenarioSortType(): ScenarioSortType =
    when (this) {
        R.id.button_name -> ScenarioSortType.NAME
        R.id.button_recent -> ScenarioSortType.RECENT
        R.id.button_most_used -> ScenarioSortType.MOST_USED
        else -> throw IllegalArgumentException("Invalid scenario sort button id")
    }