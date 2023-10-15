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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.activities

import android.app.Application
import android.content.Intent

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.base.extensions.queryIntentActivitiesCompat
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.getActivityDisplayInfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * View model for the [ActivitySelectionDialog].
 * @param application the Android application.
 */
class ActivitySelectionModel(application: Application) : AndroidViewModel(application) {

    /** Retrieves the list of activities visible on the Android launcher. */
    val activities = flow {
        val resolveInfoList = application.packageManager.queryIntentActivitiesCompat(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0,
        )

        emit(
            resolveInfoList
                .mapNotNull { it.getActivityDisplayInfo(application.packageManager) }
                .sortedBy { it.name.lowercase() }
        )
    }.flowOn(Dispatchers.IO)
}