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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.getDisplayNameResId
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toAreaDisplayText

import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Inject

class TextConditionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }
        .filterIsInstance<ScreenCondition.Text>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<TextConditionUiState?> = configuredCondition
        .map { colorCondition -> colorCondition.toUiState(context) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    @OptIn(FlowPreview::class)
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)


    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()

    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    fun setTextToDetect(text: String) {
        updateEditedCondition { it.copy(text = text) }
    }

    fun toggleShouldBeDetected() {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(shouldBeDetected = !oldCondition.shouldBeDetected)
        }
    }

    fun setDetectionArea(area: Rect) {
        updateEditedCondition {
            it.copy(detectionArea = area)
        }
    }

    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.TEXT_CONDITION_DIALOG_BUTTON_SAVE, view)
    }

    fun monitorDetectionAreaSelectorView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_AREA_SELECTOR, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.TEXT_CONDITION_DIALOG_BUTTON_SAVE)
        monitoredViewsManager.detach(MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_AREA_SELECTOR)
    }

    private fun updateEditedCondition(closure: (oldValue: ScreenCondition.Text) -> ScreenCondition.Text?) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Text>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    private fun ScreenCondition.Text.toUiState(context: Context): TextConditionUiState =
        TextConditionUiState(
            canBeSaved = isComplete(),
            name = name,
            nameError = name.isEmpty(),
            textToSearch = text,
            shouldBeDetectedChecked = shouldBeDetected,
            detectionAreaDescription = detectionArea.toAreaDisplayText(context),
            detectionAreaError = detectionArea.isEmpty,
            detectionThreshold = threshold,
            alphabetDesc = context.getString(alphabet.getDisplayNameResId()),
        )
}