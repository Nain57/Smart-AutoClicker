/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.SelectorState
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlin.math.max
import kotlin.math.min

@OptIn(FlowPreview::class)
class ConditionViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the database. */
    private val repository = Repository.getRepository(application)
    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Monitor the views fot the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedConditionState
        .mapNotNull { it.value }

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    /** The type of detection currently selected by the user. */
    val name: Flow<String?> = configuredCondition.map { it.name }.take(1)
    /** Tells if the condition name is valid or not. */
    val nameError: Flow<Boolean> = configuredCondition.map { it.name.isEmpty() }

    private val shouldBeDetectedItem = DropdownItem(
        title = R.string.dropdown_item_title_condition_visibility_present,
        helperText = R.string.dropdown_helper_text_condition_visibility_present,
        icon = R.drawable.ic_confirm,
    )
    private val shouldNotBeDetectedItem = DropdownItem(
        title= R.string.dropdown_item_title_condition_visibility_absent,
        helperText = R.string.dropdown_helper_text_condition_visibility_absent,
        icon = R.drawable.ic_cancel,
    )
    /**  Should be detected choices for the dropdown field. */
    val shouldBeDetectedItems = listOf(shouldBeDetectedItem, shouldNotBeDetectedItem)
    /** Tells if the condition should be present or not on the screen. */
    val shouldBeDetected: Flow<DropdownItem> = configuredCondition
        .mapNotNull { condition ->
            when (condition.shouldBeDetected) {
                true -> shouldBeDetectedItem
                false -> shouldNotBeDetectedItem
            }
        }
        .filterNotNull()

    val detectionTypeExact = DropdownItem(
        title = R.string.dropdown_item_title_detection_type_exact,
        icon = R.drawable.ic_detect_exact,
    )
    val detectionTypeScreen = DropdownItem(
        title= R.string.dropdown_item_title_detection_type_screen,
        icon = R.drawable.ic_detect_whole_screen,
    )
    val detectionTypeInArea = DropdownItem(
        title= R.string.dropdown_item_title_detection_type_in_area,
        icon = R.drawable.ic_detect_in_area,
    )
    /** Detection types choices for the dropdown field. */
    val detectionTypeItems = listOf(detectionTypeExact, detectionTypeScreen, detectionTypeInArea)
    /** The type of detection currently selected by the user. */
    val detectionType: Flow<DetectionTypeState> = configuredCondition
        .map { condition ->
            when (condition.detectionType) {
                EXACT -> application.getExactDetectionTypeState(condition.area)
                WHOLE_SCREEN -> application.getWholeScreenDetectionTypeState()
                IN_AREA -> application.getInAreaDetectionTypeState(condition.detectionArea ?: condition.area)
                else -> null
            }
        }
        .filterNotNull()

    /** The condition threshold value currently edited by the user. */
    val threshold: Flow<Int> = configuredCondition.mapNotNull { it.threshold }
    /** The bitmap for the configured condition. */
    val conditionBitmap: Flow<Bitmap?> = configuredCondition.map { condition ->
        if (condition.bitmap != null) return@map condition.bitmap

        condition.path?.let { path ->
            repository.getBitmap(path, condition.area.width(), condition.area.height())
        }
    }
    /** Tells if the configured condition is valid and can be saved. */
    val conditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedConditionState.map { condition ->
        condition.canBeSaved
    }

    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            editionRepository.updateEditedCondition(condition.copy(name = name))
        }
    }

    /** Set the shouldBeDetected value of the condition. */
    fun setShouldBeDetected(newShouldBeDetected: DropdownItem) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            val shouldBeDetected = when (newShouldBeDetected) {
                shouldBeDetectedItem -> true
                shouldNotBeDetectedItem -> false
                else -> return
            }

            editionRepository.updateEditedCondition(condition.copy(shouldBeDetected = shouldBeDetected))
        }
    }

    /** Set the detection type. */
    fun setDetectionType(newType: DropdownItem) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            when (newType) {
                detectionTypeExact -> editionRepository.updateEditedCondition(
                    condition.copy(detectionType = EXACT)
                )
                detectionTypeScreen -> editionRepository.updateEditedCondition(
                    condition.copy(detectionType = WHOLE_SCREEN)
                )
                detectionTypeInArea -> editionRepository.updateEditedCondition(
                    condition.copy(
                        detectionType = IN_AREA,
                        detectionArea = condition.detectionArea ?: condition.area,
                    )
                )
                else -> return
            }
        }
    }

    /** Set the area to detect in. */
    fun setDetectionArea(area: Rect) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            editionRepository.updateEditedCondition(
                condition.copy(detectionArea = sanitizeAreaForCondition(area, condition.area))
            )
        }
    }

    /**
     * Set the threshold of the configured condition.
     * @param value the new threshold value.
     */
    fun setThreshold(value: Int) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            editionRepository.updateEditedCondition(condition.copy(threshold = value))
        }
    }

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()


    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE, view)
    }

    fun monitorDetectionTypeDropdownView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CONDITION_DIALOG_DROPDOWN_DETECTION_TYPE, view)
    }

    fun monitorDropdownItemWholeScreenView(view: View) {
        monitoredViewsManager.attach(
            MonitoredViewType.CONDITION_DIALOG_DROPDOWN_ITEM_WHOLE_SCREEN,
            view,
            ViewPositioningType.SCREEN,
        )
    }

    fun stopDropdownItemWholeScreenViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.CONDITION_DIALOG_DROPDOWN_ITEM_WHOLE_SCREEN)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE)
        monitoredViewsManager.detach(MonitoredViewType.CONDITION_DIALOG_DROPDOWN_DETECTION_TYPE)
        monitoredViewsManager.detach(MonitoredViewType.CONDITION_DIALOG_DROPDOWN_ITEM_WHOLE_SCREEN)
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

    private fun Context.getExactDetectionTypeState(area: Rect) = DetectionTypeState(
        dropdownItem = detectionTypeExact,
        selectorState = SelectorState(
            isClickable = false,
            title = getString(R.string.item_title_detection_type_exact),
            subText = getString(R.string.item_desc_detection_type_exact, area.left, area.top, area.right, area.bottom),
            iconRes = null,
        )
    )

    private fun Context.getWholeScreenDetectionTypeState() = DetectionTypeState(
        dropdownItem = detectionTypeScreen,
        selectorState = SelectorState(
            isClickable = false,
            title = getString(R.string.item_title_detection_type_screen),
            subText = null,
            iconRes = null,
        )
    )

    private fun Context.getInAreaDetectionTypeState(area: Rect) = DetectionTypeState(
        dropdownItem = detectionTypeInArea,
        selectorState = SelectorState(
            isClickable = true,
            title = getString(R.string.item_title_detection_type_in_area),
            subText = getString(R.string.item_desc_detection_type_in_area, area.left, area.top, area.right, area.bottom),
            iconRes = null,
        )
    )
}

data class DetectionTypeState(
    val dropdownItem: DropdownItem,
    val selectorState: SelectorState,
)

/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20f