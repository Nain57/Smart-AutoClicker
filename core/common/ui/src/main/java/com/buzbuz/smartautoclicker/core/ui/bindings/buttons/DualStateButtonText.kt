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

import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeButtonMultiStateBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeButtonTextDualStateBinding
import com.google.android.material.button.MaterialButtonToggleGroup

/**
 * Configuration for the button represented by [IncludeButtonMultiStateBinding].
 *
 * @param textLeft text for the left button, should be short.
 * @param selectionRequired text for the right button, should be short.
 * @param singleSelection true to allow multiple buttons selections, false to allow only one.
 */
data class DualStateButtonTextConfig(
    val textLeft: String,
    val textRight: String,
    val selectionRequired: Boolean,
    val singleSelection: Boolean = true,
)


fun IncludeButtonTextDualStateBinding.setup(config: DualStateButtonTextConfig) {
    buttonLeft.text = config.textLeft
    buttonRight.text = config.textRight

    root.isSingleSelection = config.singleSelection
    root.isSelectionRequired = config.selectionRequired
}

fun IncludeButtonTextDualStateBinding.setChecked(checkedId: Int?) {
    val newCheckedViewId = getButtonViewIdFromCheckedId(checkedId)
    if (checkedId == root.checkedButtonId) return

    if (newCheckedViewId == null) root.clearChecked()
    else root.check(newCheckedViewId)
}

fun IncludeButtonTextDualStateBinding.setOnCheckedListener(listener: ((Int?) -> Unit)?) {
    if (listener == null) {
        val registeredListener = root.tag
        if (registeredListener is MaterialButtonToggleGroup.OnButtonCheckedListener) {
            root.removeOnButtonCheckedListener(registeredListener)
        }
        return
    }

    val buttonListener = MaterialButtonToggleGroup.OnButtonCheckedListener { group, checkedViewId, isChecked ->
        if (!isChecked) {
            if (group.checkedButtonId < 0) listener(null)
            return@OnButtonCheckedListener
        }

        when (checkedViewId) {
            buttonLeft.id -> listener(0)
            buttonRight.id -> listener(1)
        }
    }

    root.addOnButtonCheckedListener(buttonListener)
    root.tag = buttonListener
}

private fun IncludeButtonTextDualStateBinding.getButtonViewIdFromCheckedId(checkedId: Int?): Int? =
    when (checkedId) {
        0 -> buttonLeft.id
        1 -> buttonRight.id
        else -> null
    }
