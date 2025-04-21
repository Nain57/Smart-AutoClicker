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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text

import android.content.Context
import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.common.DetectionTypeState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.common.getDetectionTypeState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.common.sanitizeAreaForCondition

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject


class TextConditionDialogViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value as? TextCondition }

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    @OptIn(FlowPreview::class)
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    val name: Flow<String?> = configuredCondition.map { it.name }.take(1)
    val nameError: Flow<Boolean> = configuredCondition.map { it.name.isEmpty() }

    val textToDetect: Flow<String?> = configuredCondition.map { it.textToDetect }.take(1)
    val textToDetectError: Flow<Boolean> = configuredCondition.map { it.textToDetect.isEmpty() }

    val textLanguage: Flow<TextLanguagesChoice> = configuredCondition.map { TextLanguagesChoice(it.textLanguage) }

    /** Tells if the condition should be present or not on the screen. */
    val shouldBeDetected: Flow<Boolean> = configuredCondition
        .map { condition -> condition.shouldBeDetected }

    /** The type of detection currently selected by the user. */
    val detectionType: Flow<DetectionTypeState> = configuredCondition
        .map { condition ->
            condition.detectionArea?.let { area ->
                context.getDetectionTypeState(condition.detectionType, area)
            }
        }
        .filterNotNull()

    /** The condition threshold value currently edited by the user. */
    val threshold: Flow<Int> = configuredCondition.mapNotNull { it.threshold }

    /** Tells if the configured condition is valid and can be saved. */
    val conditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedScreenConditionState.map { condition ->
        condition.canBeSaved
    }

    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value


    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    fun setTextToDetect(textToDetect: String) {
        updateEditedCondition { it.copy(textToDetect = textToDetect) }
    }

    fun setTextLanguage(textLanguagesChoice: TextLanguagesChoice) {
        updateEditedCondition { it.copy(textLanguage = textLanguagesChoice.language) }
    }

    fun toggleShouldBeDetected() {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(shouldBeDetected = !oldCondition.shouldBeDetected)
        }
    }

    fun setDetectionType(newType: Int) {
        if (newType == EXACT) return
        updateEditedCondition { oldCondition ->oldCondition.copy(detectionType = newType) }
    }

    fun setDetectionArea(area: Rect) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(detectionArea = area.sanitizeAreaForCondition())
        }
    }

    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()


    private fun updateEditedCondition(closure: (oldValue: TextCondition) -> TextCondition?) {
        editionRepository.editionState.getEditedCondition<TextCondition>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }
}
