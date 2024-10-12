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
import android.view.KeyEvent
import android.view.LayoutInflater

import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper

import com.buzbuz.smartautoclicker.core.base.extensions.WindowManagerCompat
import com.buzbuz.smartautoclicker.core.common.overlays.R
import com.buzbuz.smartautoclicker.core.common.overlays.base.BaseOverlay
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.DialogBaseMoveToBinding
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.utils.getDynamicColorsContext

import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MoveToDialog(
    @StyleRes theme: Int,
    private val defaultValue: Int,
    private val itemCount: Int,
    private val onValueSelected: ((Int) -> Unit),
) : BaseOverlay(theme, recreateOnRotation = true) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseMoveToBinding

    /** Tells if the dialog is visible. */
    private var isShown = false
    private var dialog: AlertDialog? = null

    override fun onCreate() {
        viewBinding = DialogBaseMoveToBinding.inflate(LayoutInflater.from(context)).apply {
            fieldMoveToIndex.apply {
                textField.filters = arrayOf(MinMaxInputFilter(min = 1, max = itemCount))

                setLabel(R.string.dialog_move_to_position_label)
                setText(defaultValue.toString(), InputType.TYPE_CLASS_NUMBER)
                textField.setHint("Max: $itemCount")
                setOnTextChangedListener { updatePositiveButtonState() }
            }
        }

        dialog = MaterialAlertDialogBuilder(context.getDynamicColorsContext(R.style.AppTheme))
            .setTitle(R.string.dialog_move_to_title)
            .setView(viewBinding.root)
            .setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    this@MoveToDialog.back()
                    true
                } else {
                    false
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> validateCurrentValueAndClose() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> back() }
            .create()

        dialog?.window?.setType(WindowManagerCompat.TYPE_COMPAT_OVERLAY)
    }

    override fun onStart() {
        if (isShown) return

        isShown = true
        dialog?.show()

        viewBinding.fieldMoveToIndex.textField.requestFocus()
    }

    override fun onStop() {
        if (!isShown) return

        dialog?.hide()
        isShown = false
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
    }

    private fun updatePositiveButtonState() {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
            viewBinding.fieldMoveToIndex.textField.text?.getEditedValue() != null
    }

    private fun validateCurrentValueAndClose() {
        viewBinding.fieldMoveToIndex.textField.text?.getEditedValue()?.let { value ->
            onValueSelected(value)
            back()
        }
    }

    private fun Editable.getEditedValue(): Int? =
        try {
            toString().toInt()
        } catch (nfEx: NumberFormatException) {
            null
        }
}