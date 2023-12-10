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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.flags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class FlagsSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val isStartActivitiesFlags: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val selectedFlags: MutableStateFlow<Int> = MutableStateFlow(0)

    private val allAndroidFlags: Flow<List<AndroidIntentFlag>> = isStartActivitiesFlags.mapNotNull { isStartActivity ->
        isStartActivity ?: return@mapNotNull null

        (if (isStartActivity == true) getStartActivityIntentFlags() else getBroadcastIntentFlags())
            .sortedBy { flag -> flag.displayName }
    }

    val flagsItems: Flow<List<ItemFlag>> =
        combine(allAndroidFlags, selectedFlags) { allFlags, selection ->
            allFlags.map { androidFlag ->
                ItemFlag(
                    flag = androidFlag,
                    isSelected = (selection and androidFlag.value) != 0,
                )
            }
        }

    fun getSelectedFlags(): Int =
        selectedFlags.value

    fun setSelectedFlags(flags: Int, startActivityFlags: Boolean) {
        isStartActivitiesFlags.value = startActivityFlags
        selectedFlags.value = flags
    }

    fun setFlagState(flag: Int, isSelected: Boolean) {
        selectedFlags.value =
            if (isSelected) selectedFlags.value or flag
            else selectedFlags.value and flag.inv()
    }
}

data class ItemFlag(
    val flag: AndroidIntentFlag,
    val isSelected: Boolean,
)