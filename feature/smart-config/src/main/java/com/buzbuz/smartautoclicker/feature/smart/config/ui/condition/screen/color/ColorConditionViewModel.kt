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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color

import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions.getBlueValue
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions.getGreenValue
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions.getRedValue
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions.toRgbaHexString

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

class ColorConditionViewModel  @Inject constructor(
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel()  {

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }
        .filterIsInstance<ScreenCondition.Color>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<ColorConditionUiState?> = configuredCondition
        .map { colorCondition -> colorCondition.toUiState() }
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

    fun setColor(@ColorInt colorInt: Int) {
        updateEditedCondition { it.copy(color = colorInt) }
    }

    fun setPosition(position: PointF) {
        updateEditedCondition {
            val x = position.x.toInt()
            val y = position.y.toInt()
            it.copy(detectionArea = Rect(x, y, x + 1, y + 1))
        }
    }

    fun toggleShouldBeDetected() {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(shouldBeDetected = !oldCondition.shouldBeDetected)
        }
    }

    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE)
    }

    private fun updateEditedCondition(closure: (oldValue: ScreenCondition.Color) -> ScreenCondition.Color?) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    private fun ScreenCondition.Color.toUiState(): ColorConditionUiState =
        ColorConditionUiState(
            canBeSaved = isComplete(),
            conditionName = name,
            conditionNameError = name.isEmpty(),
            conditionColor = color,
            conditionColorText = color.toRgbaHexString(),
            conditionPosition = PointF(detectionArea.left.toFloat(), detectionArea.top.toFloat()),
            redValue = color.getRedValue(),
            greenValue = color.getGreenValue(),
            blueValue = color.getBlueValue(),
            shouldBeDetectedChecked = shouldBeDetected,
            detectionThreshold = threshold,
        )
}