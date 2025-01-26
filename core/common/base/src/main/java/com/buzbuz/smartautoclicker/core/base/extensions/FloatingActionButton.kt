/*
 * Copyright (C) 2025 Kevin Buzeau
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
