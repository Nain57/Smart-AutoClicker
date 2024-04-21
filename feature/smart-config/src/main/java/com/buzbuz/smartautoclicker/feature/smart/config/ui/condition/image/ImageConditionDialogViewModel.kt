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

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.SelectorState
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
                EXACT -> context.getExactDetectionTypeState(condition.area)
                WHOLE_SCREEN -> context.getWholeScreenDetectionTypeState()
                IN_AREA -> context.getInAreaDetectionTypeState(condition.detectionArea ?: condition.area)
                else -> null
            }
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

    val canTryCondition: Flow<Boolean> = configuredCondition
        .map { it.isComplete() }

    fun getTryInfo(): Pair<Scenario, ImageCondition>? {
        val scenario = editionRepository.editionState.getScenario() ?: return null
        val condition = editionRepository.editionState.getEditedCondition<ImageCondition>() ?: return null

        return scenario to condition
    }


    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    /** Set the shouldBeDetected value of the condition. */
    fun setShouldBeDetected(newShouldBeDetected: DropdownItem) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(
                shouldBeDetected = when (newShouldBeDetected) {
                    shouldBeDetectedItem -> true
                    shouldNotBeDetectedItem -> false
                    else -> return@updateEditedCondition null
                }
            )
        }
    }

    /** Set the detection type. */
    fun setDetectionType(newType: DropdownItem) {
        updateEditedCondition { oldCondition ->
            when (newType) {
                detectionTypeExact -> oldCondition.copy(detectionType = EXACT)
                detectionTypeScreen -> oldCondition.copy(detectionType = WHOLE_SCREEN)
                detectionTypeInArea -> oldCondition.copy(
                    detectionType = IN_AREA,
                    detectionArea = oldCondition.detectionArea ?: oldCondition.area,
                )
                else -> return@updateEditedCondition null
            }
        }
    }

    /** Set the area to detect in. */
    fun setDetectionArea(area: Rect) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(detectionArea = sanitizeAreaForCondition(area, oldCondition.area))
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

    private fun updateEditedCondition(closure: (oldValue: ImageCondition) -> ImageCondition?) {
        editionRepository.editionState.getEditedCondition<ImageCondition>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
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