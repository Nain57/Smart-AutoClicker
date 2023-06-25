/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays

import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics

abstract class OverlayFullScreenView : BaseOverlay() {

    /** The layout parameters of the menu layout. */
    private val viewLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        DisplayMetrics.TYPE_COMPAT_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT)

    /** The Android window manager. Used to add/remove the overlay menu and view. */
    private lateinit var windowManager: WindowManager

    private lateinit var view: View

    protected abstract fun onCreateView(layoutInflater: LayoutInflater): View

    final override fun onCreate() {
        windowManager = context.getSystemService(WindowManager::class.java)!!
        view = onCreateView(context.getSystemService(LayoutInflater::class.java))
    }

    override fun onStart() {
        windowManager.addView(view, viewLayoutParams)
    }

    override fun onStop() {
        windowManager.removeView(view)
    }
}