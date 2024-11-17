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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup

import com.buzbuz.smartautoclicker.core.base.extensions.safeStartActivity
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayMenuBackToPreviousBinding
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.common.overlays.R


open class ActivityStarterOverlayMenu(
    private val intent: Intent,
    private val fallbackIntent: Intent? = null,
) : OverlayMenu() {

    private var cannotStart: Boolean = false

    override fun onCreate() {
        super.onCreate()

        if (context.safeStartActivity(intent)) return

        val fallback = fallbackIntent
        if (fallback != null && context.safeStartActivity(fallback)) return

        Log.e(TAG, "Can't start any of the activities")
        cannotStart = true
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        OverlayMenuBackToPreviousBinding.inflate(layoutInflater).root

    override fun onMenuItemClicked(viewId: Int) {
        if (viewId == R.id.btn_back) back()
    }
}

private const val TAG = "ActivityStarterOverlayMenu"