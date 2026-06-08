/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.reference

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.GetCounterReadReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.model.CounterReference
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.GetCounterWriteReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.toUiAction
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiTriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.reference.CounterReferenceDialog.ReferencesType.*

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CounterReferenceViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getCounterReadReferencesUseCase: GetCounterReadReferencesUseCase,
    private val getCounterWriteReferencesUseCase: GetCounterWriteReferencesUseCase,
) : ViewModel() {

    private val dialogArgs = MutableStateFlow<Pair<String, CounterReferenceDialog.ReferencesType>?>(null)
    private val references: Flow<Set<CounterReference>> = dialogArgs
        .filterNotNull()
        .flatMapLatest { (counterName, type) ->
            when (type) {
                READ -> getCounterReadReferencesUseCase().map { references -> references[counterName] ?: emptySet() }
                WRITE -> getCounterWriteReferencesUseCase().map { references -> references[counterName] ?: emptySet()  }
            }
        }

    val uiState: StateFlow<List<CounterReferenceUiItem>?> = references
        .map { refs -> refs.map { ref -> ref.toUiItem(context) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setDialogArgs(name: String, type: CounterReferenceDialog.ReferencesType) {
        dialogArgs.update { name to type }
    }

    private fun CounterReference.toUiItem(context: Context): CounterReferenceUiItem {
        val eventName = event.name
        val eventIcon = when (event) {
            is ScreenEvent -> R.drawable.ic_screen_event
            is TriggerEvent -> R.drawable.ic_trigger_event
        }

        return when (this) {
            is CounterReference.ActionElement -> {
                val actionName = action.name ?: ""
                CounterReferenceUiItem(
                    elementIconRes = action.getIconRes(),
                    elementName = actionName,
                    eventIconRes = eventIcon,
                    eventName = eventName,
                    referenceDesc = action.toUiAction(context, event, !action.isComplete()).description,
                )
            }

            is CounterReference.ConditionElement -> {
                when (val cond = condition) {
                    is ScreenCondition -> {
                        val uiCond = cond.toUiScreenCondition(context, shortThreshold = false, inError = !cond.isComplete())
                        CounterReferenceUiItem(
                            elementIconRes = uiCond.detectionTypeIconRes,
                            elementName = uiCond.name,
                            eventIconRes = eventIcon,
                            eventName = eventName,
                            referenceDesc = uiCond.thresholdText,
                        )
                    }

                    is TriggerCondition -> {
                        val uiCond = cond.toUiTriggerCondition(context, inError = !cond.isComplete())
                        CounterReferenceUiItem(
                            elementIconRes = uiCond.iconRes,
                            elementName = uiCond.name,
                            eventIconRes = eventIcon,
                            eventName = eventName,
                            referenceDesc = uiCond.description,
                        )
                    }
                }
            }
        }
    }
}
