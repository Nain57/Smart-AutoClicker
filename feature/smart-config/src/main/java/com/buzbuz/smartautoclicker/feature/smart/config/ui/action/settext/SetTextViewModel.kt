/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.settext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.common.actions.text.appendCounterReference
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification

import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification.NotificationDialogUiState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification.toImportanceItem

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

import javax.inject.Inject
import kotlin.text.isNullOrEmpty


class SetTextViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel()  {

    /** The action being configured by the user. */
    private val configuredSetTextAction = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<SetText>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    @OptIn(FlowPreview::class)
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    val uiState: StateFlow<SetTextUiState?> =
        combine(configuredSetTextAction, editedActionHasChanged) { setText, hasChanged ->
            setText.toUiState(hasChanged)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    fun hasUnsavedModifications(): Boolean =
        editedActionHasChanged.value

    fun setName(newName: String) {
        editionRepository.editionState.getEditedAction<SetText>()?.let { setTextAction ->
            editionRepository.updateEditedAction(setTextAction.copy(name = "" + newName))
        }
    }

    fun setTextToWrite(newText: String) {
        editionRepository.editionState.getEditedAction<SetText>()?.let { setTextAction ->
            editionRepository.updateEditedAction(setTextAction.copy(text = "" + newText))
        }
    }

    fun appendCounterReferenceToTextToWrite(counterName: String) {
        editionRepository.editionState.getEditedAction<SetText>()?.let { setTextAction ->
            editionRepository.updateEditedAction(
                setTextAction.copy(text = "" + setTextAction.text.appendCounterReference(counterName))
            )
        }
    }

    fun toggleValidateInput() {
        editionRepository.editionState.getEditedAction<SetText>()?.let { setTextAction ->
            editionRepository.updateEditedAction(
                setTextAction.copy(validateInput = !setTextAction.validateInput)
            )
        }
    }

    private fun SetText.toUiState(hasUnsavedModifications: Boolean): SetTextUiState =
        SetTextUiState(
            canBeSaved = isComplete(),
            hasUnsavedModifications = hasUnsavedModifications,
            name = name ?: "",
            nameError = name.isNullOrEmpty(),
            textToWrite = text,
            validateInput = validateInput,
        )
}