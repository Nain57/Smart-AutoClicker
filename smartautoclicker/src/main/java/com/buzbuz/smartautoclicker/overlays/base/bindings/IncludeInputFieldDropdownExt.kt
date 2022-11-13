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
package com.buzbuz.smartautoclicker.overlays.base.bindings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.databinding.IncludeInputFieldDropdownBinding
import com.buzbuz.smartautoclicker.databinding.ItemDropdownBinding

fun IncludeInputFieldDropdownBinding.setItems(
    label: String,
    items: List<DropdownItem>,
    onItemSelected: (DropdownItem) -> Unit
) {
    layoutInput.hint = label
    textField.setAdapter(
        DropdownAdapter(items) { selectedItem ->
            textField.dismissDropDown()
            onItemSelected(selectedItem)
        }
    )
}

fun IncludeInputFieldDropdownBinding.setSelectedItem(item: DropdownItem) {
    textField.setText(textField.resources.getString(item.title), false)
}

class DropdownAdapter(
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
            root.setText(item.title)
        }

        return itemBinding.root
    }

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults = FilterResults()
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
    }
    override fun getFilter(): Filter = filter
}

data class DropdownItem(@StringRes val title: Int)