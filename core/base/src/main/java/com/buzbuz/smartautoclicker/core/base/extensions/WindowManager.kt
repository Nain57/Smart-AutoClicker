/*
 * Copyright (C) 2024 Kevin Buzeau
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

import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager

import java.lang.reflect.Field


object WindowManagerCompat {

    /** WindowManager LayoutParams type for a window over applications. */
    @Suppress("DEPRECATION")
    @JvmField
    val TYPE_COMPAT_OVERLAY =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

}

fun WindowManager.safeAddView(view: View?, params: WindowManager.LayoutParams?): Boolean {
    if (view == null || params == null) return false

    return try {
        addView(view, params)
        true
    } catch (ex: WindowManager.BadTokenException) {
        Log.e(TAG, "Can't add view to window manager, permission is denied !")
        false
    }
}

fun WindowManager.LayoutParams.disableMoveAnimations() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        setCanPlayMoveAnimation(false)
    } else {
        val wp = WindowManager.LayoutParams()
        val className = "android.view.WindowManager\$LayoutParams"
        try {
            val layoutParamsClass = Class.forName(className)
            val noAnimFlagField: Field = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")
            layoutParamsClass.getField("privateFlags").apply {
                setInt(wp, getInt(wp) or noAnimFlagField.getInt(wp))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't disable move animations !")
        }
    }
}

private const val TAG = "WindowManagerExt"