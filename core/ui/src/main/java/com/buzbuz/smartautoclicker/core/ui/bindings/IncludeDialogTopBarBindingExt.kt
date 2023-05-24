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

import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDialogNavigationTopBarBinding

fun IncludeDialogNavigationTopBarBinding.setButtonEnabledState(buttonType: com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton, enabled: Boolean) {
    when (buttonType) {
        com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.SAVE -> buttonSave.isEnabled = enabled
        com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.DISMISS -> buttonDismiss.isEnabled = enabled
        com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.DELETE -> buttonDelete.isEnabled = enabled
    }
}

fun IncludeDialogNavigationTopBarBinding.setButtonVisibility(buttonType: com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton, visibility: Int) {
    when (buttonType) {
        com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.SAVE -> buttonSave.visibility = visibility
        com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.DISMISS -> buttonDismiss.visibility = visibility
        com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.DELETE -> buttonDelete.visibility = visibility
    }
}

enum class DialogNavigationButton {
    DISMISS,
    DELETE,
    SAVE,
}