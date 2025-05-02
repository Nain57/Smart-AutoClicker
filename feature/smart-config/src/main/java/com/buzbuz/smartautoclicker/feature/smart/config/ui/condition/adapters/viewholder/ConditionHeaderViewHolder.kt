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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.buzbuz.smartautoclicker.core.ui.databinding.ItemListHeaderBinding
import com.buzbuz.smartautoclicker.core.ui.recyclerview.ViewBindingHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionHeader


internal class ConditionHeaderViewHolder(
    parent: ViewGroup,
): ViewBindingHolder<ItemListHeaderBinding>(
    viewBinding = ItemListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
) {

    fun onBind(header: UiConditionHeader) {
        viewBinding.textHeader.setText(header.title)
    }
}