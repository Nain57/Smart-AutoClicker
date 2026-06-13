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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.GetActionMissingReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.GetConditionMissingReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.ReplaceMissingCounterReferenceUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.ReplaceMissingEventToggleReferenceUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.ReplaceMissingScreenConditionReferenceUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.toUiAction
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixEventsChildrenCopyUiState

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class FixEventChildrenCopyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getActionMissingReferencesUseCase: GetActionMissingReferencesUseCase,
    private val getConditionMissingReferencesUseCase: GetConditionMissingReferencesUseCase,
    private val replaceMissingCounterReferenceUseCase: ReplaceMissingCounterReferenceUseCase,
    private val replaceMissingEventToggleReferenceUseCase: ReplaceMissingEventToggleReferenceUseCase,
    private val replaceMissingScreenConditionReferenceUseCase: ReplaceMissingScreenConditionReferenceUseCase
) : ViewModel() {

    private val itemsToCopy: MutableStateFlow<FixEventChildrenCopyDialog.Arguments?> = MutableStateFlow(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FixEventsChildrenCopyUiState?> = itemsToCopy
        .mapLatest { args -> args.toUiState(context) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setDialogArguments(arguments: FixEventChildrenCopyDialog.Arguments) {
        itemsToCopy.update { arguments }
    }

    /** Get the list of possible replacements for a */
    fun getScreenConditionReplacementCandidates(context: Context): List<UiScreenCondition> {
        val event = itemsToCopy.value?.parent
        if (event == null || event !is ScreenEvent) return emptyList()

        val conditions = event.conditions

        return conditions.map { condition ->
            condition.toUiScreenCondition(context = context, shortThreshold = true, inError = false)
        }
    }

    fun updateEventToggles(
        item: FixCopyUiItem.Item.EventChildren,
        missingRef: MissingCopyReference.EventToggleReference,
        replacement: List<EventToggle>,
    ) {
        if (item !is FixCopyUiItem.Item.EventChildren.ActionItem) return

        val newEvent = replaceMissingEventToggleReferenceUseCase(
            event = itemsToCopy.value?.parent ?: return,
            itemToEdit = item.itemWithMissingReferences,
            missingReference = missingRef,
            replacement = replacement,
        )

        itemsToCopy.update { old -> old?.copy(parent = newEvent) }
    }

    fun updateScreenCondition(
        item: FixCopyUiItem.Item.EventChildren,
        missingRef: MissingCopyReference.ScreenConditionReference,
        replacement: ScreenCondition,
    ) {
        if (item !is FixCopyUiItem.Item.EventChildren.ActionItem) return

        val newEvent = replaceMissingScreenConditionReferenceUseCase(
            event = itemsToCopy.value?.parent ?: return,
            itemToEdit = item.itemWithMissingReferences,
            missingReference = missingRef,
            replacement = replacement,
        )

        itemsToCopy.update { old -> old?.copy(parent = newEvent) }
    }

    fun updateCounter(
        item: FixCopyUiItem.Item.EventChildren,
        missingRef: MissingCopyReference.CounterReference,
        replacement: String,
    ) {
        val newEvent = replaceMissingCounterReferenceUseCase(
            event = itemsToCopy.value?.parent ?: return,
            itemToEdit = item.itemWithMissingReferences,
            missingReference = missingRef,
            replacement = replacement,
        )

        itemsToCopy.update { old -> old?.copy(parent = newEvent) }
    }

    fun getFixedEventToCopy(): Event? =
        itemsToCopy.value?.parent

    private suspend fun FixEventChildrenCopyDialog.Arguments?.toUiState(context: Context): FixEventsChildrenCopyUiState {
        val items = this?.let { (_, event, showHelpMessage) ->
            buildList {
                if (showHelpMessage) add(FixCopyUiItem.Header(R.string.item_header_event_copy_fix))
                addAll(event.conditions.toUiState(context) + event.actions.toUiState(context))
            }
        } ?: emptyList()

        val canBeCopied = items
            .filterIsInstance<FixCopyUiItem.Item.EventChildren>()
            .all { it.isValidForCopy }

        return FixEventsChildrenCopyUiState(canBeCopied = canBeCopied, items = items)
    }

    private fun List<Condition>.toUiState(context: Context): List<FixCopyUiItem.Item.EventChildren.ConditionItem> =
        map { condition ->
            val result = getConditionMissingReferencesUseCase(condition)
            val isValid = result.missingReferences.isEmpty()
            FixCopyUiItem.Item.EventChildren.ConditionItem(
                uiCondition = condition.toUiCondition(context, inError = false),
                stateText = context.getStateText(isValid),
                isValidForCopy = isValid,
                itemWithMissingReferences = result,
            )
        }

    private suspend fun List<Action>.toUiState(context: Context): List<FixCopyUiItem.Item.EventChildren.ActionItem> =
        map { action ->
            val result = getActionMissingReferencesUseCase(action)
            val isValid = result.missingReferences.isEmpty()
            FixCopyUiItem.Item.EventChildren.ActionItem(
                uiAction = action.toUiAction(context, inError = false),
                stateText = context.getStateText(isValid),
                isValidForCopy = isValid,
                itemWithMissingReferences = result,
            )
        }

    private fun Context.getStateText(isValid: Boolean): String =
        if (isValid) getString(R.string.item_copy_fix_description_ok)
        else getString(R.string.item_copy_fix_description_missing)
}
