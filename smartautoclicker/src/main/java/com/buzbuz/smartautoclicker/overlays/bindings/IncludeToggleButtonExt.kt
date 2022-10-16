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
import com.buzbuz.smartautoclicker.R

import com.buzbuz.smartautoclicker.databinding.IncludeToggleButtonBinding


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
    setCheckedButton(newCheckedButtonId)
    if (buttonGroup.checkedButtonId != newCheckedButtonId) {
        buttonGroup.check(newCheckedButtonId)
    }
    descriptionText.setText(description)
}

fun IncludeToggleButtonBinding.setChecked(@IdRes newCheckedButtonId: Int, description: String) {
    setCheckedButton(newCheckedButtonId)
    if (buttonGroup.checkedButtonId != newCheckedButtonId) {
        buttonGroup.check(newCheckedButtonId)
    }
    descriptionText.text = description
}

private fun IncludeToggleButtonBinding.setCheckedButton(@IdRes newCheckedButtonId: Int) {
    when (newCheckedButtonId) {
        R.id.left_button -> {
            leftButton.setIconResource(R.drawable.ic_confirm)
            rightButton.setIconResource(0)
        }

        R.id.right_button -> {
            leftButton.setIconResource(0)
            rightButton.setIconResource(R.drawable.ic_confirm)
        }
    }
}