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
package com.buzbuz.smartautoclicker.overlays.bindings

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.view.children

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.IncludeToggleButtonBinding

import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup


fun IncludeToggleButtonBinding.setButtonsText(@StringRes left: Int, @StringRes right: Int) {
    leftButton.setText(left)
    rightButton.setText(right)
}

fun IncludeToggleButtonBinding.addOnCheckedListener(listener: (checkedId: Int) -> Unit) {
    buttonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
        if (!isChecked) return@addOnButtonCheckedListener
        listener(checkedId)
    }
}

fun IncludeToggleButtonBinding.setChecked(@IdRes newCheckedButtonId: Int, @StringRes description: Int) {
    buttonGroup.setChecked(newCheckedButtonId)
    descriptionText.setText(description)
}

fun IncludeToggleButtonBinding.setChecked(@IdRes newCheckedButtonId: Int, description: String) {
    buttonGroup.setChecked(newCheckedButtonId)
    descriptionText.text = description
}

fun MaterialButtonToggleGroup.setChecked(@IdRes newCheckedButtonId: Int) {
    children.forEach { button ->
        (button as MaterialButton).setIconResource(
            if (button.id == newCheckedButtonId) R.drawable.ic_confirm
            else 0
        )
    }

    if (checkedButtonId != newCheckedButtonId) {
        check(newCheckedButtonId)
    }
}