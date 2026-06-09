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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy

import android.content.Context
import android.graphics.Bitmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.GetScreenConditionsForCopyUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.GetTriggerConditionsForCopyUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ConditionsForCopy
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiTriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** View model for the [ConditionCopyDialog]. */
class ConditionCopyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    getScreenConditionsForCopyUseCase: GetScreenConditionsForCopyUseCase,
    getTriggerConditionsForCopyUseCase: GetTriggerConditionsForCopyUseCase,
    private val bitmapRepository: BitmapRepository,
) : ViewModel() {

    private val requestTriggerConditions: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    /** The currently searched action name. Null if none is. */
    private val searchQuery = MutableStateFlow<String?>(null)
    private val checkedForCopy: MutableStateFlow<Map<Identifier, ConditionWithIndex>> = MutableStateFlow(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val conditionsToCopy: Flow<ConditionsForCopy<out Condition>> = requestTriggerConditions
        .flatMapLatest { isRequestingTriggerConditions ->
            if (isRequestingTriggerConditions == true) getTriggerConditionsForCopyUseCase()
            else getScreenConditionsForCopyUseCase()
        }

    private val allCopyItems: Flow<ConditionCopyUiState> =
        combine(conditionsToCopy, checkedForCopy) { conditions, checked ->
            ConditionCopyUiState(
                items = buildList {
                    if (conditions.thisEvent.isNotEmpty()) {
                        add(ConditionCopyItem.HeaderItem(R.string.list_header_copy_conditions_this))
                        addAll(conditions.thisEvent.toCopyItems(context, checked).sortedBy { it.uiCondition.name })
                    }
                    if (conditions.thisScenario.isNotEmpty()) {
                        add(ConditionCopyItem.HeaderItem(R.string.list_header_copy_conditions_this_scenario))
                        addAll(conditions.thisScenario.toCopyItems(context, checked).sortedBy { it.uiCondition.name })
                    }
                    if (conditions.otherScenario.isNotEmpty()) {
                        add(ConditionCopyItem.HeaderItem(R.string.list_header_copy_conditions_all))
                        addAll(conditions.otherScenario.toCopyItems(context, checked).sortedBy { it.uiCondition.name })
                    }
                },
                thisEventSize = if (conditions.thisEvent.isNotEmpty()) conditions.thisEvent.size + 1 else 0,
                thisScenarioSize = if (conditions.thisScenario.isNotEmpty()) conditions.thisScenario.size + 1 else 0,
                otherScenarioSize = if (conditions.otherScenario.isNotEmpty()) conditions.otherScenario.size + 1 else 0,
            )
        }

    val uiState: StateFlow<ConditionCopyUiState?> = allCopyItems
        .combine(searchQuery) { state, query ->
            if (query.isNullOrEmpty()) state
            else state.copy(
                items = state.items
                    .filterIsInstance<ConditionCopyItem.ConditionItem>()
                    .filter { item -> item.uiCondition.name.contains(query, true) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setCopyListType(triggerConditions: Boolean) {
        requestTriggerConditions.update { triggerConditions }
    }

    fun toggleCheckedForCopy(condition: Condition, index: Int) {
        checkedForCopy.update { old ->
            if (old.contains(condition.id)) old - condition.id
            else old + (condition.id to ConditionWithIndex(condition, index))
        }
    }

    fun conditionCopyShouldWarnUser(): Boolean {
        val state = uiState.value ?: return false

        // If there is only one condition that references something unreachable, we should warn.
        // So return early we one is found.
        checkedForCopy.value.forEach { (_, conditionWithIndex) ->

            // It's always safe to copy from the same event
            if (state.thisEventSize > 0 && conditionWithIndex.index in 0 until state.thisEventSize) return@forEach

            // In the same scenario but different event
            val thisScenarioStartIndex = state.thisEventSize
            val otherScenarioStartIndex = thisScenarioStartIndex + state.thisScenarioSize
            if (state.thisScenarioSize > 0 && conditionWithIndex.index in thisScenarioStartIndex until otherScenarioStartIndex) {
                if (conditionWithIndex.condition.isRelatedToItsEvent()) return true
                return@forEach
            }

            // In the same scenario but different event
            val otherScenarioEndIndex = otherScenarioStartIndex + state.otherScenarioSize - 1
            if (state.otherScenarioSize > 0 && conditionWithIndex.index in otherScenarioStartIndex .. otherScenarioEndIndex) {
                if (conditionWithIndex.condition.isRelatedToItsScenario()) return true
                return@forEach
            }
        }

        return false
    }

    fun getConditionsToCopy(): List<Condition> = checkedForCopy.value
        .mapNotNull { (_, conditionWithIndex) -> conditionWithIndex.condition }

    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(bitmapRepository, condition, onBitmapLoaded)

    private fun List<Condition>.toCopyItems(context: Context, checked: Map<Identifier, ConditionWithIndex>) = map { condition ->
        when (condition) {
            is ScreenCondition -> ConditionCopyItem.ConditionItem.Screen(
                uiCondition = condition.toUiScreenCondition(context, shortThreshold = true, inError = !condition.isComplete()),
                isChecked = checked.contains(condition.id),
            )
            is TriggerCondition -> ConditionCopyItem.ConditionItem.Trigger(
                uiCondition = condition.toUiTriggerCondition(context, inError = !condition.isComplete()),
                isChecked = checked.contains(condition.id),
            )
        }
    }

    private fun Condition.isRelatedToItsEvent(): Boolean =
        when (this) {
            // No conditions relates to an event specific value
            is ScreenCondition.Color,
            is ScreenCondition.Image,
            is ScreenCondition.Text,
            is ScreenCondition.Number,
            is TriggerCondition.OnBroadcastReceived,
            is TriggerCondition.OnCounterCountReached,
            is TriggerCondition.OnTimerReached -> false
        }

    private fun Condition.isRelatedToItsScenario(): Boolean =
        when (this) {
            // Number condition can be compared to a counter
            is ScreenCondition.Number -> counterValue is CounterOperationValue.Counter
            // On counter reached condition always refers to a counter
            is TriggerCondition.OnCounterCountReached -> true

            is ScreenCondition.Color,
            is ScreenCondition.Image,
            is ScreenCondition.Text,
            is TriggerCondition.OnBroadcastReceived,
            is TriggerCondition.OnTimerReached -> false
        }

    private data class ConditionWithIndex(
        val condition: Condition,
        val index: Int,
    )
}