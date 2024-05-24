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
package com.buzbuz.smartautoclicker.core.ui.bindings.buttons

import android.view.View
import androidx.core.content.ContextCompat
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeButtonMultiStateBinding
import com.google.android.material.button.MaterialButtonToggleGroup

/**
 * Configuration for the button represented by [IncludeButtonMultiStateBinding].
 *
 * @param icons the icons in the buttons. Must contains 2 or 3 elements.
 * @param selectionRequired true to force a button to be selected, false to allow null selection.
 * @param singleSelection true to allow multiple buttons selections, false to allow only one.
 */
data class MultiStateButtonConfig(
    val icons: List<Int>,
    val selectionRequired: Boolean,
    val singleSelection: Boolean = true,
)


fun IncludeButtonMultiStateBinding.setup(state: MultiStateButtonConfig) {
    if (state.icons.size !in 2..3) throw IllegalArgumentException("Button should have 2 or 3 entries")

    buttonLeft.icon = ContextCompat.getDrawable(root.context, state.icons[0])
    buttonMiddle.icon = ContextCompat.getDrawable(root.context, state.icons[1])

    if (state.icons.size > 2) {
        buttonRight.icon = ContextCompat.getDrawable(root.context, state.icons[2])
        buttonRight.visibility = View.VISIBLE
    } else {
        buttonRight.visibility = View.GONE
    }

    root.isSingleSelection = state.singleSelection
    root.isSelectionRequired = state.selectionRequired
}

fun IncludeButtonMultiStateBinding.setChecked(checkedId: Int?) {
    val newCheckedViewId = getButtonViewIdFromCheckedId(checkedId)
    if (checkedId == root.checkedButtonId) return

    if (newCheckedViewId == null) root.clearChecked()
    else root.check(newCheckedViewId)
}

fun IncludeButtonMultiStateBinding.setOnCheckedListener(listener: ((Int?) -> Unit)?) {
    val registeredListener = root.tag
    if (registeredListener is MaterialButtonToggleGroup.OnButtonCheckedListener) {
        root.removeOnButtonCheckedListener(registeredListener)
    }

    if (listener == null) {
        return
    }

    val buttonListener = MaterialButtonToggleGroup.OnButtonCheckedListener { group, checkedViewId, isChecked ->
        if (!isChecked) {
            if (group.checkedButtonId < 0) listener(null)
            return@OnButtonCheckedListener
        }

        when (checkedViewId) {
            buttonLeft.id -> listener(0)
            buttonMiddle.id -> listener(1)
            buttonRight.id -> listener(2)
        }
    }

    root.addOnButtonCheckedListener(buttonListener)
    root.tag = buttonListener
}

private fun IncludeButtonMultiStateBinding.getButtonViewIdFromCheckedId(checkedId: Int?): Int? =
    when (checkedId) {
        0 -> buttonLeft.id
        1 -> buttonMiddle.id
        2 -> buttonRight.id
        else -> null
    }
