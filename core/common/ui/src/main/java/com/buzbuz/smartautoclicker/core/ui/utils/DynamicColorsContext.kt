
package com.buzbuz.smartautoclicker.core.ui.utils

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.DynamicColors


fun Context.getDynamicColorsContext(@StyleRes theme: Int): Context =
    DynamicColors.wrapContextIfAvailable(ContextThemeWrapper(this, theme))