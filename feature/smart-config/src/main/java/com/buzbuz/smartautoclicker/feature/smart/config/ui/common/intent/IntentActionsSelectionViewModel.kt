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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.intent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.android.intent.AndroidIntentApi
import com.buzbuz.smartautoclicker.core.android.intent.getBroadcastReceptionIntentActions
import com.buzbuz.smartautoclicker.core.android.intent.getStartActivityIntentActions
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class IntentActionsSelectionViewModel @Inject constructor() : ViewModel() {

    private val selectedAction: MutableStateFlow<String?> = MutableStateFlow(null)
    private val getBroadcastActions: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    private val startActivityActions: Flow<List<AndroidIntentApi<String>>> = flow {
        emit(getStartActivityIntentActions().sortedBy { flag -> flag.displayName })
    }
    private val receiveBroadcastActions: Flow<List<AndroidIntentApi<String>>> = flow {
        emit(getBroadcastReceptionIntentActions().sortedBy { flag -> flag.displayName })
    }

    private val requestedActions: Flow<List<AndroidIntentApi<String>>> = getBroadcastActions
        .filterNotNull()
        .flatMapLatest { getBroadcast ->
            if (getBroadcast) receiveBroadcastActions
            else startActivityActions
        }

    val actionsItems: Flow<List<ItemAction>> =
        combine(requestedActions, selectedAction) { allActions, selection ->
            allActions.map { androidAction ->
                ItemAction(
                    action = androidAction,
                    isSelected = androidAction.value == selection,
                )
            }
        }

    fun setRequestedActionsType(requestBroadcast: Boolean) {
        viewModelScope.launch { getBroadcastActions.emit(requestBroadcast) }
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