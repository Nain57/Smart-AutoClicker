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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters

import androidx.recyclerview.widget.DiffUtil
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionHeader
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionItem

internal object UiConditionItemDiffUtilCallback : DiffUtil.ItemCallback<UiConditionItem>() {

    override fun areItemsTheSame(oldItem: UiConditionItem, newItem: UiConditionItem): Boolean =  when {
        oldItem is UiConditionHeader && newItem is UiConditionHeader -> true
        oldItem is UiCondition && newItem is UiCondition ->
            oldItem::class.java == newItem::class.java && oldItem.condition.id == newItem.condition.id
        else -> false
    }

    override fun areContentsTheSame(oldItem: UiConditionItem, newItem: UiConditionItem): Boolean = oldItem == newItem
}