/*
 * Copyright (C) 2020 Nain57
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
@file:Suppress("DEPRECATION")

package com.buzbuz.smartautoclicker.extensions

import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

/** WindowManager LayoutParams type for a window over applications. */
@JvmField val TYPE_COMPAT_OVERLAY =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LayoutParams.TYPE_APPLICATION_OVERLAY
    else LayoutParams.TYPE_SYSTEM_OVERLAY

/** The size of the current display in pixels. */
val WindowManager.displaySize : Point
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            currentWindowMetrics.bounds.size()
        } else {
            val size = Point()
            defaultDisplay.getRealSize(size)
            size
        }

/** Get the left and top inset of the default display. */
val WindowManager.leftTopInsets : Point?
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            defaultDisplay.cutout?.let {
                Point(it.safeInsetLeft, it.safeInsetTop)
            }
        } else {
            null
        }

