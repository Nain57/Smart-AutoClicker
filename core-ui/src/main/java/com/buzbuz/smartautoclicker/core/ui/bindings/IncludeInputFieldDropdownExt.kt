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
package com.buzbuz.smartautoclicker.core.ui.bindings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeInputFieldDropdownBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.ItemDropdownBinding

import com.google.android.material.textfield.TextInputLayout

fun IncludeInputFieldDropdownBinding.setItems(
    items: List<DropdownItem>,
    onItemSelected: (DropdownItem) -> Unit,
    label: String? = null,
    enabled: Boolean = true,
    @DrawableRes disabledIcon: Int? = null,
    onDisabledClick: (() -> Unit)? = null,
) {
    textLayout.apply {
        if (enabled) {
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
        } else {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            disabledIcon?.let { setEndIconDrawable(it) }
        }

        isHintEnabled = label != null
        hint = label
    }

    textField.setAdapter(
        DropdownAdapter(items) { selectedItem ->
            textField.dismissDropDown()
            onItemSelected(selectedItem)
        }
    )

    if (enabled) {
        disabledTouchHandler.visibility = View.GONE
    } else {
        onDisabledClick?.let {
            disabledTouchHandler.apply {
                visibility = View.VISIBLE
                setOnClickListener { it() }
            }
        }
    }
}

fun IncludeInputFieldDropdownBinding.setSelectedItem(item: DropdownItem) {
    textField.setText(textField.resources.getString(item.title), false)

    textLayout.apply {
        if (item.helperText != null) {
            isHelperTextEnabled = true
            helperText = resources.getString(item.helperText)
        } else {
            isHelperTextEnabled = false
        }

        if (item.icon != null) setStartIconDrawable(item.icon)
    }
}

data class DropdownItem(
    @StringRes val title: Int,
    @StringRes val helperText: Int? = null,
    @DrawableRes val icon: Int? = null,
)

private class DropdownAdapter(
    private val items: List<DropdownItem>,
    private val onItemSelected: (DropdownItem) -> Unit,
) : Filterable, BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): DropdownItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val itemBinding: ItemDropdownBinding =
            if (convertView == null) ItemDropdownBinding.inflate(LayoutInflater.from(parent!!.context), parent, false)
            else ItemDropdownBinding.bind(convertView)
        val item = getItem(position)

        itemBinding.apply {
            root.setOnClickListener { onItemSelected(item) }
            dropdownItemText.setText(item.title)

            dropdownItemIcon.apply {
                if (item.icon != null) {
                    setImageResource(item.icon)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                    setImageDrawable(null)
                }
            }
        }

        return itemBinding.root
    }

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults = FilterResults()
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
    }
    override fun getFilter(): Filter = filter
}