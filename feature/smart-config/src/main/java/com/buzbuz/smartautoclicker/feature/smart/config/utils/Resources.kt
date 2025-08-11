
package com.buzbuz.smartautoclicker.feature.smart.config.utils

import android.widget.ImageView
import androidx.annotation.ColorInt
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.google.android.material.textfield.TextInputLayout


/** @param color the tint color to apply to the ImageView. */
fun ImageView.setIconTintColor(@ColorInt color: Int) {
    setColorFilter(
        color,
        android.graphics.PorterDuff.Mode.SRC_IN
    )
}

fun TextInputLayout.setError(isError: Boolean) {
    error = if (isError) context.getString(R.string.input_field_error_required) else null
}

/** Check if this duration value is valid for an action. */
fun Long?.isValidDuration(): Boolean = this != null && this > 0L

const val ALPHA_DISABLED_ITEM = 0.5f
const val ALPHA_DISABLED_ITEM_INT = 127
const val ALPHA_ENABLED_ITEM = 1f
const val ALPHA_ENABLED_ITEM_INT = 255
