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
package com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation

import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes

import com.buzbuz.smartautoclicker.core.common.overlays.R
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.DialogBaseMoveToBinding
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter

import com.google.android.material.bottomsheet.BottomSheetDialog

class MoveToDialog(
    @StyleRes theme: Int,
    private val defaultValue: Int,
    private val itemCount: Int,
    private val onValueSelected: ((Int) -> Unit),
) : OverlayDialog(theme) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseMoveToBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseMoveToBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.text = context.getString(R.string.dialog_move_to_title)
                buttonDelete.visibility = View.GONE
                buttonSave.visibility = View.VISIBLE

                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.setDebouncedOnClickListener {
                    fieldMoveToIndex.textField.text?.getEditedValue()?.let { value ->
                        onValueSelected(value)
                        back()
                    }
                }
            }

            fieldMoveToIndex.apply {
                textField.filters = arrayOf(MinMaxInputFilter(min = 1, max = itemCount))
                setLabel(R.string.dialog_move_to_position_label)
                setText(defaultValue.toString(), InputType.TYPE_CLASS_NUMBER)
                setOnTextChangedListener { editable ->
                    val value = editable.getEditedValue()
                    viewBinding.layoutTopBar.setButtonEnabledState(
                        DialogNavigationButton.SAVE,
                        value != null && value >= 1 && value <= itemCount,
                    )
                }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) = Unit

    private fun Editable.getEditedValue(): Int? =
        try {
            toString().toInt()
        } catch (nfEx: NumberFormatException) {
            null
        }
}