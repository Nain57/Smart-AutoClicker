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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent

import android.content.Context
import android.content.Intent

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * View model for the [ActivitySelectionDialog].
 * @param context the Android context.
 */
class ActivitySelectionModel(context: Context) : OverlayViewModel(context) {

    /** Retrieves the list of activities visible on the Android launcher. */
    val activities = flow {
        val resolveInfoList = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0,
        )

        emit(
            resolveInfoList
                .mapNotNull { it.getActivityDisplayInfo(context.packageManager) }
                .sortedBy { it.name.lowercase() }
        )
    }.flowOn(Dispatchers.IO)
}