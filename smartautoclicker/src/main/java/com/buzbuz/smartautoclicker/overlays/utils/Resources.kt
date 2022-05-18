/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.utils

import android.widget.ImageView

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action

/**
 * Get the icon for a given action.
 * @return the icon resource identifier.
 */
@DrawableRes
fun Action.getIconRes() : Int =
    when (this) {
        is Action.Click -> R.drawable.ic_click
        is Action.Swipe -> R.drawable.ic_swipe
        is Action.Pause -> R.drawable.ic_wait_aligned
        is Action.Intent -> R.drawable.ic_intent
    }

/** @param color the tint color to apply to the ImageView. */
fun ImageView.setIconTint(@ColorRes color: Int) {
    setColorFilter(
        ContextCompat.getColor(context, color),
        android.graphics.PorterDuff.Mode.SRC_IN
    )
}
