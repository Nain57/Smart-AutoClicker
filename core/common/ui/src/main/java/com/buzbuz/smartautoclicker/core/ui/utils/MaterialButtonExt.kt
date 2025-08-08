
package com.buzbuz.smartautoclicker.core.ui.utils

import androidx.annotation.ColorInt
import com.buzbuz.smartautoclicker.core.ui.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable


fun MaterialButton.showProgress(@ColorInt tintColor: Int = iconTint.defaultColor) {
    icon = IndeterminateDrawable.createCircularDrawable(
        context,
        CircularProgressIndicatorSpec(
            context,
            null,
            0,
            R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall
        ).apply { indicatorColors = intArrayOf(tintColor) },
    )
}

fun MaterialButton.hideProgress() {
    icon = null
}