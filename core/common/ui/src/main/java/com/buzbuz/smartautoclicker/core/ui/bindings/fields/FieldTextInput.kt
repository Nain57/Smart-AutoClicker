
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import android.text.Editable
import android.text.InputType
import android.view.inputmethod.EditorInfo

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldTextInputBinding
import com.buzbuz.smartautoclicker.core.ui.utils.OnAfterTextChangedListener


fun IncludeFieldTextInputBinding.setLabel(@StringRes labelResId: Int) {
    root.setHint(labelResId)
}

fun IncludeFieldTextInputBinding.setText(text: String?, type: Int = InputType.TYPE_CLASS_TEXT) {
    textField.apply {
        inputType = type
        imeOptions = EditorInfo.IME_ACTION_DONE
        setText(text)
    }
}

fun IncludeFieldTextInputBinding.setError(isError: Boolean) {
    setError(R.string.input_field_error_required, isError)
}

fun IncludeFieldTextInputBinding.setError(@StringRes messageId: Int, isError: Boolean) {
    root.error = if (isError) root.context.getString(messageId) else null
}

fun IncludeFieldTextInputBinding.setOnTextChangedListener(listener: (Editable) -> Unit) {
    textField.addTextChangedListener(OnAfterTextChangedListener(listener))
}