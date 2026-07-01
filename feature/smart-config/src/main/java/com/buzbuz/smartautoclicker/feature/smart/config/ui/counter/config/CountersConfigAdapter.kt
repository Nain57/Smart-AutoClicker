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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.config

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemCounterConfigBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString

import com.google.android.material.textfield.TextInputLayout

/**
 * Adapter for the list of counters in the configuration.
 *
 * @param onExpandCollapse Click listener for the expand/collapse button.
 * @param onSetByClick Click listener for the "Set by" button.
 * @param onReadByClick Click listener for the "Read by" button.
 * @param onDeleteClick Click listener for the delete button.
 * @param onStartingValueChange Listener for when the starting value is changed.
 */
class CountersConfigAdapter(
    private val onExpandCollapse: (CounterUiItem) -> Unit,
    private val onSetByClick: (CounterUiItem) -> Unit,
    private val onReadByClick: (CounterUiItem) -> Unit,
    private val onDeleteClick: (CounterUiItem) -> Unit,
    private val onCounterClicked: (CounterUiItem) -> Unit,
    private val onStartingValueChange: (CounterUiItem, Double) -> Unit,
    private val onCancelReplace: () -> Unit,
) : ListAdapter<CounterUiItem, CountersConfigViewHolder>(CounterDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountersConfigViewHolder =
        CountersConfigViewHolder(
            binding = ItemCounterConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onExpandCollapse = onExpandCollapse,
            onSetByClick = onSetByClick,
            onReadByClick = onReadByClick,
            onDeleteClick = onDeleteClick,
            onCounterClicked = onCounterClicked,
            onStartingValueChange = onStartingValueChange,
            onCancelReplace = onCancelReplace,
        )

    override fun onBindViewHolder(holder: CountersConfigViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object CounterDiffCallback : DiffUtil.ItemCallback<CounterUiItem>() {

    override fun areItemsTheSame(oldItem: CounterUiItem, newItem: CounterUiItem): Boolean =
        oldItem.counterName == newItem.counterName

    // Exclude user input derived values to avoid blinking while editing
    override fun areContentsTheSame(oldItem: CounterUiItem, newItem: CounterUiItem): Boolean =
        oldItem.isExpanded == newItem.isExpanded &&
        oldItem.selectedForReplacement == newItem.selectedForReplacement &&
        oldItem.setByButtonText == newItem.setByButtonText &&
        oldItem.setByButtonIsEmpty == newItem.setByButtonIsEmpty &&
        oldItem.readByButtonText == newItem.readByButtonText &&
        oldItem.readByButtonIsEmpty == newItem.readByButtonIsEmpty &&
        oldItem.deleteButtonText == newItem.deleteButtonText &&
        oldItem.deleteButtonEnabled == newItem.deleteButtonEnabled
}

class CountersConfigViewHolder(
    private val binding: ItemCounterConfigBinding,
    onExpandCollapse: (CounterUiItem) -> Unit,
    onSetByClick: (CounterUiItem) -> Unit,
    onReadByClick: (CounterUiItem) -> Unit,
    onDeleteClick: (CounterUiItem) -> Unit,
    onCounterClicked: (CounterUiItem) -> Unit,
    onStartingValueChange: (CounterUiItem, Double) -> Unit,
    onCancelReplace: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private var item: CounterUiItem? = null

    init {
        binding.apply {
            contentLayout.setOnClickListener { item?.let(onCounterClicked) }
            buttonExpandCollapse.setOnClickListener { item?.let(onExpandCollapse) }
            writtenByButton.setOnClickListener { item?.let(onSetByClick) }
            readByButton.setOnClickListener { item?.let(onReadByClick) }
            deleteButton.setOnClickListener { item?.let(onDeleteClick) }
            replaceByText.setOnClickListener { onCancelReplace() }

            layoutStartingValue.textLayout.hint = root.context.getString(R.string.field_label_counter_starting_value)
            layoutStartingValue.textLayout.endIconMode = TextInputLayout.END_ICON_NONE
            layoutStartingValue.setOnTextChangedListener { text ->
                val counter = item ?: return@setOnTextChangedListener
                val newValue = text.toString().toDoubleOrNull() ?: return@setOnTextChangedListener
                if (newValue != counter.startingValue) {
                    onStartingValueChange(counter, newValue)
                }
            }
        }
    }

    fun bind(newItem: CounterUiItem) {
        item = newItem
        binding.apply {
            counterName.text = newItem.counterName
            counterDesc.text = newItem.counterDesc

            if (newItem.isExpanded) {
                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_up)
                layoutStartingValue.root.visibility = View.VISIBLE
                writtenByButton.visibility = View.VISIBLE
                readByButton.visibility = View.VISIBLE
                deleteButton.visibility = View.VISIBLE

                val startingValueText = newItem.startingValue.toNaturalDisplayString()
                layoutStartingValue.setText(startingValueText, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            } else {
                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_down)
                layoutStartingValue.root.visibility = View.GONE
                writtenByButton.visibility = View.GONE
                readByButton.visibility = View.GONE
                deleteButton.visibility = View.GONE
            }

            writtenByButton.apply {
                text = newItem.setByButtonText
                isEnabled = !newItem.setByButtonIsEmpty
            }
            readByButton.apply {
                text = newItem.readByButtonText
                isEnabled = !newItem.readByButtonIsEmpty
            }

            deleteButton.text = newItem.deleteButtonText
            deleteButton.isEnabled = newItem.deleteButtonEnabled

            replaceByText.visibility = if (newItem.selectedForReplacement) View.VISIBLE else View.GONE
        }
    }
}
