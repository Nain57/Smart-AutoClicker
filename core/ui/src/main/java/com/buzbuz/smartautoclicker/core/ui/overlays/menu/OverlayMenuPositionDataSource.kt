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

import androidx.annotation.VisibleForTesting

internal class OverlayMenuPositionDataSource private constructor(context: Context) {

    internal companion object {
        /** Name of the preference file. */
        @VisibleForTesting const val PREFERENCE_NAME = "OverlayMenuController"
        /** Preference key referring to the landscape X position. */
        @VisibleForTesting const val PREFERENCE_MENU_X_LANDSCAPE_KEY = "Menu_X_Landscape_Position"
        /** Preference key referring to the landscape Y position. */
        @VisibleForTesting const val PREFERENCE_MENU_Y_LANDSCAPE_KEY = "Menu_Y_Landscape_Position"
        /** Preference key referring to the portrait X position. */
        @VisibleForTesting const val PREFERENCE_MENU_X_PORTRAIT_KEY = "Menu_X_Portrait_Position"
        /** Preference key referring to the portrait Y position. */
        @VisibleForTesting const val PREFERENCE_MENU_Y_PORTRAIT_KEY = "Menu_Y_Portrait_Position"

        /** Singleton preventing multiple instances of the OverlayMenuPositionDataSource at the same time. */
        @Volatile
        private var INSTANCE: OverlayMenuPositionDataSource? = null

        /**
         * Get the OverlayMenuPositionDataSource singleton, or instantiates it if it wasn't yet.
         *
         * @return the OverlayMenuPositionDataSource singleton.
         */
        fun getInstance(context: Context): OverlayMenuPositionDataSource {
            return INSTANCE ?: synchronized(this) {
                val instance = OverlayMenuPositionDataSource(context)
                INSTANCE = instance
                instance
            }
        }
    }

    /** The shared preference storing the position of the menu in order to save/restore the last user position. */
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    private val onLockPositionChangedListeners: MutableSet<(Point?) -> Unit> = mutableSetOf()

    /**
     * Tells if the position have been locked by [lockPosition].
     * If not null, the position will not change until [unlockPosition] is called.
     */
    private var lockedMenuPosition: Point? = null

    /**
     * Load last user menu position for the current orientation, if any.
     *
     * @param orientation the orientation to load the position for.
     */
    fun loadMenuPosition(orientation: Int): Point? {
        lockedMenuPosition?.let { lockedPosition ->
            return lockedPosition
        }

        val position = when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> Point(
                sharedPreferences.getInt(PREFERENCE_MENU_X_LANDSCAPE_KEY, 0),
                sharedPreferences.getInt(PREFERENCE_MENU_Y_LANDSCAPE_KEY, 0),
            )
            Configuration.ORIENTATION_PORTRAIT -> Point(
                sharedPreferences.getInt(PREFERENCE_MENU_X_PORTRAIT_KEY, 0),
                sharedPreferences.getInt(PREFERENCE_MENU_Y_PORTRAIT_KEY, 0),
            )
            else -> return null
        }

        Log.d(TAG, "loadMenuPosition for orientation $orientation = [${position.x}, ${position.y}]")
        return position
    }

    /**
     * Save the last user menu position for the current orientation.
     *
     * @param orientation the orientation to save the position for.
     */
    fun saveMenuPosition(position: Point, orientation: Int) {
        if (isPositionLocked()) return

        Log.d(TAG, "saveMenuPosition for orientation $orientation = $position")

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            sharedPreferences.edit()
                .putInt(PREFERENCE_MENU_X_LANDSCAPE_KEY, position.x)
                .putInt(PREFERENCE_MENU_Y_LANDSCAPE_KEY, position.y)
                .apply()
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            sharedPreferences.edit()
                .putInt(PREFERENCE_MENU_X_PORTRAIT_KEY, position.x)
                .putInt(PREFERENCE_MENU_Y_PORTRAIT_KEY, position.y)
                .apply()
        }
    }

    fun addOnLockedPositionChangedListener(listener: (Point?) -> Unit) {
        onLockPositionChangedListeners.add(listener)
    }

    fun removeOnLockedPositionChangedListener(listener: (Point?) -> Unit) {
        onLockPositionChangedListeners.remove(listener)
    }

    fun lockPosition(position: Point) {
        if (isPositionLocked()) return

        notifyOnLockedPositionChanged(position)
        lockedMenuPosition = position
    }

    fun unlockPosition() {
        if (!isPositionLocked()) return

        lockedMenuPosition = null
        notifyOnLockedPositionChanged(null)
    }

    private fun notifyOnLockedPositionChanged(position: Point?) {
        onLockPositionChangedListeners.forEach { it(position) }
    }

    private fun isPositionLocked(): Boolean =
        lockedMenuPosition != null
}

private const val TAG = "OverlayMenuPositionDataSource"