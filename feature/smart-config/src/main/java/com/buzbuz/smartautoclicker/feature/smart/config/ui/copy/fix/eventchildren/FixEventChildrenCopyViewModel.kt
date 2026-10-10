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
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.GetActionMissingReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.GetConditionMissingReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.ReplaceMissingCounterReferenceUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.ReplaceMissingEventToggleReferenceUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.ReplaceMissingScreenConditionReferenceUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixEventsChildrenCopyUiState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.toFixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.toFixCopyUiState

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
    private val replaceMissingScreenConditionReferenceUseCase: ReplaceMissingScreenConditionReferenceUseCase,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val itemsToCopy: MutableStateFlow<FixEventChildrenCopyDialog.Arguments?> = MutableStateFlow(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FixEventsChildrenCopyUiState?> = itemsToCopy
        .mapLatest { args -> args.toUiItems(context).toFixCopyUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setDialogArguments(arguments: FixEventChildrenCopyDialog.Arguments) {
        itemsToCopy.update { arguments }
    }

    fun getScreenConditionReplacementCandidates(): List<ScreenCondition> {
        val event = itemsToCopy.value?.parent
        if (event == null || event !is ScreenEvent) return emptyList()

        return event.conditions
    }

    /** The copied event is not the edited one, an edition must be started for it to create correct sub elements. */
    fun startActionEdition(action: Action) {
        val event = itemsToCopy.value?.parent ?: return

        editionRepository.startEventEdition(event)
        editionRepository.startActionEdition(action)
    }

    fun stopActionEdition() {
        editionRepository.stopActionEdition()
        editionRepository.stopEventEdition()
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

    private suspend fun FixEventChildrenCopyDialog.Arguments?.toUiItems(context: Context): List<FixCopyUiItem> {
        val (resultingEvents, event) = this ?: return emptyList()

        return buildList {
            event.conditions.forEach { condition ->
                add(getConditionMissingReferencesUseCase(condition, resultingEvents).toFixCopyUiItem(context))
            }
            event.actions.forEach { action ->
                add(getActionMissingReferencesUseCase(action, resultingEvents).toFixCopyUiItem(context))
            }
        }
    }
}
