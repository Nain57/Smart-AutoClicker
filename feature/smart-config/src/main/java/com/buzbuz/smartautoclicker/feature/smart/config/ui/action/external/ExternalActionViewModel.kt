/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.ExternalAction
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@OptIn(FlowPreview::class)
class ExternalActionViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val configuredExternalAction = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<ExternalAction>()

    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    val knownExternalActionNames: Flow<List<String>> =
        editionRepository.editionState.allEditedEventsFlow
            .map { events ->
                events
                    .flatMap { event -> event.actions }
                    .filterIsInstance<ExternalAction>()
                    .map { action -> action.externalActionName.trim() }
                    .filter { name -> name.isNotEmpty() }
                    .distinct()
                    .sortedBy { name -> name.lowercase() }
            }

    val uiState: StateFlow<ExternalActionUiState?> = combine(
        configuredExternalAction,
        editionRepository.editionState.editedActionState.map { it.hasChanged },
        editionRepository.editionState.editedActionState.map { it.canBeSaved },
    ) { action, hasChanged, canBeSaved ->
        ExternalActionUiState(
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasChanged,
            name = action.name,
            nameError = action.name?.isEmpty() ?: true,
            externalActionName = action.externalActionName,
            externalActionNameError = action.externalActionName.isBlank(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    fun setName(name: String) {
        updateEditedExternalAction { old -> old.copy(name = "" + name) }
    }

    fun setExternalActionName(name: String) {
        updateEditedExternalAction { old -> old.copy(externalActionName = name.trim()) }
    }

    private fun updateEditedExternalAction(closure: (old: ExternalAction) -> ExternalAction) {
        editionRepository.editionState.getEditedAction<ExternalAction>()?.let { old ->
            editionRepository.updateEditedAction(closure(old))
        }
    }
}
