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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.common.intent

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.android.intent.AndroidIntentApi
import com.buzbuz.smartautoclicker.core.android.intent.getStartActivityIntentActions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class ActionsSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val selectedAction: MutableStateFlow<String?> = MutableStateFlow(null)

    private val allAndroidActions: Flow<List<AndroidIntentApi<String>>> = flow {
        emit(getStartActivityIntentActions().sortedBy { flag -> flag.displayName })
    }

    val actionsItems: Flow<List<ItemAction>> =
        combine(allAndroidActions, selectedAction) { allActions, selection ->
            allActions.map { androidAction ->
                ItemAction(
                    action = androidAction,
                    isSelected = androidAction.value == selection,
                )
            }
        }

    fun getSelectedAction(): String? =
        selectedAction.value

    fun setSelectedAction(action: String?) {
        selectedAction.value = action?.trim()
    }

    fun setActionSelectionState(action: String, isSelected: Boolean) {
        selectedAction.value = if (isSelected) action else null
    }
}

data class ItemAction(
    val action: AndroidIntentApi<String>,
    val isSelected: Boolean,
)