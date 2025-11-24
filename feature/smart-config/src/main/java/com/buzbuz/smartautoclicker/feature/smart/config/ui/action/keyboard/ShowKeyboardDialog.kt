package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionShowKeyboardBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.google.android.material.bottomsheet.BottomSheetDialog

class ShowKeyboardDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private lateinit var binding: DialogConfigActionShowKeyboardBinding

    override fun onCreateView(): ViewGroup {
        binding = DialogConfigActionShowKeyboardBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_showkeyboard)
                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener {
                        listener.onConfirmClicked()
                        back()
                    }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener {
                        listener.onDeleteClicked()
                        back()
                    }
                }
            }
        }
        return binding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        binding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, true)
    }

    override fun back() {
        listener.onDismissClicked()
        super.back()
    }
}