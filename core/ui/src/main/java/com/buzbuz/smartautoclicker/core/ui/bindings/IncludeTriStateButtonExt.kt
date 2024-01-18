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
package com.buzbuz.smartautoclicker.core.ui.bindings

import androidx.core.content.ContextCompat
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeTriStateButtonBinding


fun IncludeTriStateButtonBinding.setIcons(icons: List<Int>, selectionRequired: Boolean = false) {
    if (icons.size != 3) throw IllegalArgumentException("idToIcon map should have one entry for each buttons")

    button1.icon = ContextCompat.getDrawable(button1.context, icons[0])
    button2.icon = ContextCompat.getDrawable(button2.context, icons[1])
    button3.icon = ContextCompat.getDrawable(button3.context, icons[2])
    root.isSingleSelection = true
    root.isSelectionRequired = selectionRequired
}

fun IncludeTriStateButtonBinding.setChecked(checkedId: Int?) {
    val newCheckedViewId = getButtonViewIdFromCheckedId(checkedId)
    if (checkedId == root.checkedButtonId) return

    if (newCheckedViewId == null) root.clearChecked()
    else root.check(newCheckedViewId)
}

fun IncludeTriStateButtonBinding.setOnCheckedListener(listener: (Int?) -> Unit) {
    root.apply {
        addOnButtonCheckedListener { group, checkedViewId, isChecked ->
            if (!isChecked) {
                if (group.checkedButtonId < 0) listener(null)
                return@addOnButtonCheckedListener
            }

            when (checkedViewId) {
                button1.id -> listener(0)
                button2.id -> listener(1)
                button3.id -> listener(2)
            }
        }
    }
}

private fun IncludeTriStateButtonBinding.getButtonViewIdFromCheckedId(checkedId: Int?): Int? =
    when (checkedId) {
        0 -> button1.id
        1 -> button2.id
        2 -> button3.id
        else -> null
    }
