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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.legacy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.availability.IsActionCopyAvailableUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief.ActionConfigurator
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief.createNewAction
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeChoice
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.UiAction
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.toItemBriefs

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


class SmartActionsLegacyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    isActionCopyAvailableUseCase: IsActionCopyAvailableUseCase,
    private val editionRepository: EditionRepository,
) : ViewModel(), ActionConfigurator {

    val uiState: StateFlow<SmartActionsLegacyUiState?> = combine(
        editionRepository.editionState.editedEventState.mapNotNull { it.value },
        editionRepository.editionState.editedEventActionsState,
        isActionCopyAvailableUseCase(),
    ) { event, actions, canCopy ->
        SmartActionsLegacyUiState(
            actionBriefs = actions.toItemBriefs(context, event),
            isCopyButtonVisible = canCopy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // The legacy dialog exposes copy through its own button, not through the type selection
    override fun getActionTypeChoices(): List<ActionTypeChoice> = listOf(
        ActionTypeChoice.Click,
        ActionTypeChoice.Gesture,
        ActionTypeChoice.Pause,
        ActionTypeChoice.SetText,
        ActionTypeChoice.System,
        ActionTypeChoice.ChangeCounter,
        ActionTypeChoice.ToggleEvent,
        ActionTypeChoice.Notification,
        ActionTypeChoice.Intent,
    )

    override fun createAction(context: Context, choice: ActionTypeChoice): Action =
        editionRepository.createNewAction(context, choice)

    override fun startActionEdition(action: Action) {
        editionRepository.startActionEdition(action)
    }

    override fun upsertEditedAction() {
        editionRepository.upsertEditedAction()
    }

    override fun removeEditedAction() {
        editionRepository.deleteEditedAction()
    }

    override fun dismissEditedAction() {
        editionRepository.stopActionEdition()
    }

    fun updateActionOrder(actionsBrief: List<ItemBrief>) =
        editionRepository.updateActionsOrder(actionsBrief.map { brief -> (brief.data as UiAction).action })
}
