
package com.buzbuz.smartautoclicker.core.base.extensions

import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager

import java.lang.reflect.Field


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

fun WindowManager.safeUpdateViewLayout(view: View, params: WindowManager.LayoutParams?): Boolean {
    return try {
        updateViewLayout(view, params)
        true
    } catch (ex: IllegalArgumentException) {
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