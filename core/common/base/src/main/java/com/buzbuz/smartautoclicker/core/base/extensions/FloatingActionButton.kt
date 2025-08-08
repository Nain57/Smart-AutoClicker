
package com.buzbuz.smartautoclicker.core.base.extensions

import android.graphics.Rect
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton


fun FloatingActionButton.applySafeContentInsets(marginsIfInset: Rect, marginIfNot: Rect) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        windowInsets.getSafeContentInsets().let { insets ->
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = if (insets.bottom != 0) marginsIfInset.bottom + insets.bottom else marginIfNot.bottom
                rightMargin = if (insets.right != 0) marginsIfInset.right + insets.right else marginIfNot.right
                leftMargin = if (insets.left != 0) marginsIfInset.left + insets.left else marginIfNot.left
            }
        }

        WindowInsetsCompat.CONSUMED
    }
}

private fun WindowInsetsCompat.getSafeContentInsets(): Insets =
    getInsets(WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout()
    )
