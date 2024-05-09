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
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import android.text.Editable
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldTextInputWithCheckboxBinding
import com.buzbuz.smartautoclicker.core.ui.utils.OnAfterTextChangedListener


fun IncludeFieldTextInputWithCheckboxBinding.setup(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    disableInputWithCheckbox: Boolean,
) {
    editTextFieldLayout.setHint(label)
    buttonCheckbox.setIconResource(icon)
    root.tag = disableInputWithCheckbox
    buttonCheckbox.isCheckable = disableInputWithCheckbox
}

fun IncludeFieldTextInputWithCheckboxBinding.setNumericValue(value: String) {
    textField.apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        imeOptions = EditorInfo.IME_ACTION_DONE
        textField.setText(value)
    }
}

fun IncludeFieldTextInputWithCheckboxBinding.setTextValue(value: String?) {
    textField.apply {
        inputType = InputType.TYPE_CLASS_TEXT
        imeOptions = EditorInfo.IME_ACTION_DONE
        textField.setText(value)
    }
}

fun IncludeFieldTextInputWithCheckboxBinding.setChecked(isChecked: Boolean) {
    if ((root.tag as? Boolean) != true) return

    editTextFieldLayout.apply {
        isEnabled = !isChecked
        alpha = if (isChecked) DISABLED_ITEM_ALPHA else ENABLED_ITEM_ALPHA
    }
    buttonCheckbox.isChecked = isChecked
}

fun IncludeFieldTextInputWithCheckboxBinding.setButtonVisibility(isVisible: Boolean) {
    buttonCheckbox.visibility = if (isVisible) View.VISIBLE else View.GONE
}

fun IncludeFieldTextInputWithCheckboxBinding.setError(isError: Boolean) {
    setError(R.string.input_field_error_required, isError)
}

fun IncludeFieldTextInputWithCheckboxBinding.setError(@StringRes messageId: Int, isError: Boolean) {
    editTextFieldLayout.error = if (isError) root.context.getString(messageId) else null
}

fun IncludeFieldTextInputWithCheckboxBinding.setOnTextChangedListener(listener: (Editable) -> Unit) {
    textField.addTextChangedListener(OnAfterTextChangedListener(listener))
}

fun IncludeFieldTextInputWithCheckboxBinding.setOnCheckboxClickedListener(listener: () -> Unit) {
    buttonCheckbox.setOnClickListener { listener() }
}

private const val ENABLED_ITEM_ALPHA = 1f
private const val DISABLED_ITEM_ALPHA = 0.5f