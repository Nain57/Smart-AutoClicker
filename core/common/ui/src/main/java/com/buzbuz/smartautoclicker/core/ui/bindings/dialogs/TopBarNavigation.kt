
package com.buzbuz.smartautoclicker.core.ui.bindings.dialogs

import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDialogNavigationTopBarBinding

fun IncludeDialogNavigationTopBarBinding.setButtonEnabledState(buttonType: DialogNavigationButton, enabled: Boolean) {
    when (buttonType) {
        DialogNavigationButton.SAVE -> buttonSave.isEnabled = enabled
        DialogNavigationButton.DISMISS -> buttonDismiss.isEnabled = enabled
        DialogNavigationButton.DELETE -> buttonDelete.isEnabled = enabled
    }
}

fun IncludeDialogNavigationTopBarBinding.setButtonVisibility(buttonType: DialogNavigationButton, visibility: Int) {
    when (buttonType) {
        DialogNavigationButton.SAVE -> buttonSave.visibility = visibility
        DialogNavigationButton.DISMISS -> buttonDismiss.visibility = visibility
        DialogNavigationButton.DELETE -> buttonDelete.visibility = visibility
    }
}

enum class DialogNavigationButton {
    DISMISS,
    DELETE,
    SAVE,
}