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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.component

import android.content.Context

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.android.application.AndroidApplicationInfo
import com.buzbuz.smartautoclicker.core.android.application.getAllAndroidApplicationsInfo

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/** View model for the [ComponentSelectionDialog]. */
class ComponentSelectionModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    /** Retrieves the list of activities visible on the Android launcher. */
    val activities: Flow<List<AndroidApplicationInfo>> = flow {
        emit(
            getAllAndroidApplicationsInfo(context.packageManager)
            .sortedBy { it.name.lowercase() })
    }.flowOn(Dispatchers.IO)

}