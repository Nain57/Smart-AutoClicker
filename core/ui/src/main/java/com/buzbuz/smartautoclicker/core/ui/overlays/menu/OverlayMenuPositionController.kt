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
package com.buzbuz.smartautoclicker.core.ui.overlays.menu

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import android.view.View

import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics

internal class OverlayMenuPositionController(
    private val menuLayout: View,
    private val displayMetrics: DisplayMetrics,
    private val onMenuPositionChanged: (Point) -> Unit,
) {

    @VisibleForTesting
    internal companion object {
        /** Name of the preference file. */
        const val PREFERENCE_NAME = "OverlayMenuController"
        /** Preference key referring to the landscape X position. */
        const val PREFERENCE_MENU_X_LANDSCAPE_KEY = "Menu_X_Landscape_Position"
        /** Preference key referring to the landscape Y position. */
        const val PREFERENCE_MENU_Y_LANDSCAPE_KEY = "Menu_Y_Landscape_Position"
        /** Preference key referring to the portrait X position. */
        const val PREFERENCE_MENU_X_PORTRAIT_KEY = "Menu_X_Portrait_Position"
        /** Preference key referring to the portrait Y position. */
        const val PREFERENCE_MENU_Y_PORTRAIT_KEY = "Menu_Y_Portrait_Position"
    }

    /** The shared preference storing the position of the menu in order to save/restore the last user position. */
    private val sharedPreferences: SharedPreferences =
        menuLayout.context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    /** The current position of the menu. */
    private var menuPosition: Point = Point(0, 0)

    /** The initial position of the overlay menu when pressing the move menu item. */
    private var moveInitialMenuPosition: Point = Point(0, 0)
    /** The initial position of the touch event that as initiated the move of the overlay menu. */
    private var moveInitialTouchPosition: Point = Point(0, 0)

    /**
     * Tells if the position have been locked by [lockPosition].
     * If true, the position will not change until [unlockPosition] is called.
     */
    private var isPositionLocked: Boolean = false

    fun onMoveButtonTouchEvent(event: MotionEvent) =
        when {
            isPositionLocked -> false
            event.action == MotionEvent.ACTION_DOWN -> {
                moveInitialMenuPosition = Point(menuPosition.x, menuPosition.y)
                moveInitialTouchPosition = Point(event.rawX.toInt(), event.rawY.toInt())
                true
            }
            event.action == MotionEvent.ACTION_MOVE -> {
                updateMenuPosition(
                    moveInitialMenuPosition.x + (event.rawX.toInt() - moveInitialTouchPosition.x),
                    moveInitialMenuPosition.y + (event.rawY.toInt() - moveInitialTouchPosition.y)
                )
                true
            }
            else -> false
        }

    /**
     * Load last user menu position for the current orientation, if any.
     *
     * @param orientation the orientation to load the position for.
     */
    fun loadMenuPosition(orientation: Int) {
        val x: Int
        val y: Int
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                x = sharedPreferences.getInt(PREFERENCE_MENU_X_LANDSCAPE_KEY, 0)
                y = sharedPreferences.getInt(PREFERENCE_MENU_Y_LANDSCAPE_KEY, 0)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                x = sharedPreferences.getInt(PREFERENCE_MENU_X_PORTRAIT_KEY, 0)
                y = sharedPreferences.getInt(PREFERENCE_MENU_Y_PORTRAIT_KEY, 0)
            }
            else -> return
        }

        Log.d(TAG, "loadMenuPosition for orientation $orientation = [$x, $y]")

        updateMenuPosition(x, y)
    }

    /**
     * Save the last user menu position for the current orientation.
     *
     * @param orientation the orientation to save the position for.
     */
    fun saveMenuPosition(orientation: Int) {
        if (isPositionLocked) return

        Log.d(TAG, "saveMenuPosition for orientation $orientation = $menuPosition")

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            sharedPreferences.edit()
                .putInt(PREFERENCE_MENU_X_LANDSCAPE_KEY, menuPosition.x)
                .putInt(PREFERENCE_MENU_Y_LANDSCAPE_KEY, menuPosition.y)
                .apply()
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            sharedPreferences.edit()
                .putInt(PREFERENCE_MENU_X_PORTRAIT_KEY, menuPosition.x)
                .putInt(PREFERENCE_MENU_Y_PORTRAIT_KEY, menuPosition.y)
                .apply()
        }
    }

    fun lockPosition(position: Point, orientation: Int, savePosition: Boolean) {
        if (isPositionLocked) return

        if (savePosition) saveMenuPosition(orientation)
        isPositionLocked = true
        updateMenuPosition(position.x, position.y)
    }

    fun unlockPosition(orientation: Int) {
        if (!isPositionLocked) return

        isPositionLocked = false
        loadMenuPosition(orientation)
    }

    /**
     * Safe setter for the position of the overlay menu ensuring it will not be displayed outside the screen.
     *
     * @param x the horizontal position.
     * @param y the vertical position.
     */
    private fun updateMenuPosition(x: Int, y: Int) {
        val displaySize = displayMetrics.screenSize
        menuPosition = Point(
            x.coerceIn(0, displaySize.x - menuLayout.width),
            y.coerceIn(0, displaySize.y - menuLayout.height),
        )

        onMenuPositionChanged(menuPosition)
    }
}

private const val TAG = "OverlayMenuPositionController"