
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