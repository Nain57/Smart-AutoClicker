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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import kotlin.math.max

@OptIn(FlowPreview::class)
class ImageConditionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: IRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedImageConditionState
        .mapNotNull { it.value }

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedImageConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    /** The type of detection currently selected by the user. */
    val name: Flow<String?> = configuredCondition.map { it.name }.take(1)
    /** Tells if the condition name is valid or not. */
    val nameError: Flow<Boolean> = configuredCondition.map { it.name.isEmpty() }

    /** Tells if the condition should be present or not on the screen. */
    val shouldBeDetected: Flow<Boolean> = configuredCondition
        .map { condition -> condition.shouldBeDetected }

    /** The type of detection currently selected by the user. */
    val detectionType: Flow<DetectionTypeState> = configuredCondition
        .map { condition ->
            context.getDetectionTypeState(condition.detectionType, condition.detectionArea ?: condition.captureArea)
        }
        .filterNotNull()

    /** The condition threshold value currently edited by the user. */
    val threshold: Flow<Int> = configuredCondition.mapNotNull { it.threshold }
    /** The bitmap for the configured condition. */
    val conditionBitmap: Flow<Bitmap?> = configuredCondition.map { condition ->
        repository.getConditionBitmap(condition)
    }.flowOn(Dispatchers.IO)
    /** Tells if the configured condition is valid and can be saved. */
    val conditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedImageConditionState.map { condition ->
        condition.canBeSaved
    }

    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value

    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    /** Set the shouldBeDetected value of the condition. */
    fun toggleShouldBeDetected() {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(shouldBeDetected = !oldCondition.shouldBeDetected)
        }
    }

    /** Set the detection type. */
    fun setDetectionType(newType: Int) {
        updateEditedCondition { oldCondition ->
            val detectionArea =
                if (oldCondition.detectionArea == null && newType == IN_AREA) oldCondition.captureArea
                else oldCondition.detectionArea

            oldCondition.copy(detectionType = newType, detectionArea = detectionArea)
        }
    }

    /** Set the area to detect in. */
    fun setDetectionArea(area: Rect) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(detectionArea = sanitizeAreaForCondition(area, oldCondition.captureArea))
        }
    }

    /**
     * Set the threshold of the configured condition.
     * @param value the new threshold value.
     */
    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()


    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE, view)
    }

    fun monitorDetectionTypeItemWholeScreenView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CONDITION_DIALOG_FIELD_TYPE_ITEM_WHOLE_SCREEN, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE)
        monitoredViewsManager.detach(MonitoredViewType.CONDITION_DIALOG_FIELD_TYPE_ITEM_WHOLE_SCREEN)
    }

    private fun sanitizeAreaForCondition(area: Rect, conditionArea: Rect): Rect {
        val left = max(area.left, 0)
        val top = max(area.top, 0)
        val width = max(area.right - left, conditionArea.width())
        val height = max(area.bottom - top, conditionArea.height())

        return Rect(
            left,
            top,
            left + width,
            top + height,
        )
    }

    private fun updateEditedCondition(closure: (oldValue: ImageCondition) -> ImageCondition?) {
        editionRepository.editionState.getEditedCondition<ImageCondition>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    private fun Context.getDetectionTypeState(@DetectionType type: Int, area: Rect) = DetectionTypeState(
        type = type,
        areaText = getString(R.string.field_select_detection_area_desc, area.left, area.top, area.right, area.bottom)
    )
}

data class DetectionTypeState(
    @DetectionType val type: Int,
    val areaText: String,
)

/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20f