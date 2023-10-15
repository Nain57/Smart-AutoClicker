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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings

import android.text.Editable
import android.text.InputType
import android.view.inputmethod.EditorInfo

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.core.ui.utils.OnAfterTextChangedListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.IncludeTextWithInfiniteFieldBinding

fun IncludeTextWithInfiniteFieldBinding.setLabel(@StringRes labelResId: Int) {
    editTextFieldLayout.setHint(labelResId)
}
fun IncludeTextWithInfiniteFieldBinding.setRepeatCount(count: String) {
    textField.apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        imeOptions = EditorInfo.IME_ACTION_DONE
        textField.setText(count)
    }
}

fun IncludeTextWithInfiniteFieldBinding.setInfiniteState(isInfinite: Boolean) {
    if (isInfinite) {
        editTextFieldLayout.apply {
            isEnabled = false
            alpha = DISABLED_ITEM_ALPHA
        }
        buttonInfinite.isChecked = true
    } else {
        editTextFieldLayout.apply {
            isEnabled = true
            alpha = ENABLED_ITEM_ALPHA
        }
        buttonInfinite.isChecked = false
    }
}

fun IncludeTextWithInfiniteFieldBinding.setError(isError: Boolean) {
    setError(R.string.input_field_error_required, isError)
}

fun IncludeTextWithInfiniteFieldBinding.setError(@StringRes messageId: Int, isError: Boolean) {
    editTextFieldLayout.error = if (isError) root.context.getString(messageId) else null
}

fun IncludeTextWithInfiniteFieldBinding.setOnTextChangedListener(listener: (Editable) -> Unit) {
    textField.addTextChangedListener(OnAfterTextChangedListener(listener))
}

fun IncludeTextWithInfiniteFieldBinding.setOnInfiniteButtonClickedListener(listener: () -> Unit) {
    buttonInfinite.setOnClickListener { listener() }
}

private const val ENABLED_ITEM_ALPHA = 1f
private const val DISABLED_ITEM_ALPHA = 0.5f