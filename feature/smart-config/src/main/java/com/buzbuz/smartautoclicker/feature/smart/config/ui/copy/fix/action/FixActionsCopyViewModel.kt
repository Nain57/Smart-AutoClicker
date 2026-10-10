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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.action

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.GetActionMissingReferencesUseCase
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

/**
 * View model for the [FixActionsCopyDialog].
 * The actions are copied into the currently edited event, so its edition state is used as reference.
 */
class FixActionsCopyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getActionMissingReferencesUseCase: GetActionMissingReferencesUseCase,
    private val replaceMissingCounterReferenceUseCase: ReplaceMissingCounterReferenceUseCase,
    private val replaceMissingEventToggleReferenceUseCase: ReplaceMissingEventToggleReferenceUseCase,
    private val replaceMissingScreenConditionReferenceUseCase: ReplaceMissingScreenConditionReferenceUseCase,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val actionsToCopy: MutableStateFlow<List<Action>?> = MutableStateFlow(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FixEventsChildrenCopyUiState?> = actionsToCopy
        .mapLatest { actions -> actions.toUiItems(context).toFixCopyUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setActionsToCopy(actions: List<Action>) {
        actionsToCopy.update { actions }
    }

    fun getScreenConditionReplacementCandidates(): List<ScreenCondition> =
        (editionRepository.editionState.getEditedEvent() as? ScreenEvent)?.conditions ?: emptyList()

    fun getScenarioEvents(): List<Event> =
        editionRepository.editionState.getAllEditedEvents()

    /** An event edition is already running, only the action one must be started to create correct sub elements. */
    fun startActionEdition(action: Action) {
        editionRepository.startActionEdition(action)
    }

    fun stopActionEdition() {
        editionRepository.stopActionEdition()
    }

    fun updateEventToggles(
        item: FixCopyUiItem.Item.EventChildren.ActionItem,
        missingRef: MissingCopyReference.EventToggleReference,
        replacement: List<EventToggle>,
    ) {
        val newAction = replaceMissingEventToggleReferenceUseCase(item.uiAction.action, missingRef, replacement)
            ?: return
        replaceAction(item.uiAction.action, newAction)
    }

    fun updateScreenCondition(
        item: FixCopyUiItem.Item.EventChildren.ActionItem,
        missingRef: MissingCopyReference.ScreenConditionReference,
        replacement: ScreenCondition,
    ) {
        val newAction = replaceMissingScreenConditionReferenceUseCase(item.uiAction.action, missingRef, replacement)
            ?: return
        replaceAction(item.uiAction.action, newAction)
    }

    fun updateCounter(
        item: FixCopyUiItem.Item.EventChildren.ActionItem,
        missingRef: MissingCopyReference.CounterReference,
        replacement: String,
    ) {
        val newAction = replaceMissingCounterReferenceUseCase(item.uiAction.action, missingRef, replacement)
            ?: return
        replaceAction(item.uiAction.action, newAction)
    }

    fun getFixedActionsToCopy(): List<Action> =
        actionsToCopy.value ?: emptyList()

    private fun replaceAction(oldAction: Action, newAction: Action) {
        actionsToCopy.update { old ->
            old?.map { action -> if (action.id == oldAction.id) newAction else action }
        }
    }

    private suspend fun List<Action>?.toUiItems(context: Context): List<FixCopyUiItem> {
        this ?: return emptyList()

        // The edited event might not be saved in the edited list yet, use its current state for references resolution
        val editedEvents = listOfNotNull(editionRepository.editionState.getEditedEvent())

        return buildList {
            add(FixCopyUiItem.Header(R.string.item_header_event_copy_fix))
            this@toUiItems.forEach { action ->
                add(getActionMissingReferencesUseCase(action, editedEvents).toFixCopyUiItem(context))
            }
        }
    }
}
