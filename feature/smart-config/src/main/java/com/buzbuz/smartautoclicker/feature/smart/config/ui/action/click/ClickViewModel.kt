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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Point
import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putClickPressDurationConfig
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
class ClickViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: IRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()

    /** The action being configured by the user. */
    private val configuredClick = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Click>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    /** The name of the click. */
    val name: Flow<String?> = configuredClick
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredClick
        .map { it.name?.isEmpty() ?: true }

    /** The duration between the press and release of the click in milliseconds. */
    val pressDuration: Flow<String?> = configuredClick
        .map { it.pressDuration?.toString() }
        .take(1)
    /** Tells if the press duration value is valid or not. */
    val pressDurationError: Flow<Boolean> = configuredClick
        .map { (it.pressDuration ?: -1) <= 0 }

    val availableConditions: StateFlow<List<UiScreenCondition>> = editionRepository.editionState.editedEventScreenConditionsState
        .map { editedConditions ->
            editedConditions.value?.filter { it.shouldBeDetected }
                ?.map {
                    it.toUiScreenCondition(
                        context = context,
                        inError = !it.isComplete(),
                    )
                }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val positionStateUi: Flow<ClickPositionUiState?> =
        combine(editionRepository.editionState.editedEventState, configuredClick) { event, click ->
            val evt = event.value ?: return@combine null

            when {
                evt is TriggerEvent ->
                    context.getUserSelectedClickPositionState(click, forced = true)

                evt is ScreenEvent && click.positionType == Click.PositionType.USER_SELECTED ->
                    context.getUserSelectedClickPositionState(click, forced = false)

                evt is ScreenEvent && click.positionType == Click.PositionType.ON_DETECTED_CONDITION && event.value.conditionOperator == OR ->
                    context.getOnConditionWithOrPositionState(click)

                evt is ScreenEvent && click.positionType == Click.PositionType.ON_DETECTED_CONDITION && event.value.conditionOperator == AND ->
                    context.getOnConditionWithAndPositionState(evt, click)

                else -> null
            }
        }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /** Tells if the configured click is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    fun getEditedClick(): Click? =
        editionRepository.editionState.getEditedAction()

    fun hasUnsavedModifications(): Boolean =
        editedActionHasChanged.value

    /**
     * Set the name of the click.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(name = "" + name))
        }
    }

    /** Set if this click should be made on the detected condition. */
    fun setClickOnCondition(newType: Click.PositionType) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(positionType = newType))
        }
    }

    /**
     * Set the position of the click.
     * @param position the new position.
     */
    fun setPosition(position: Point) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(position = position))
        }
    }

    /**
     * Set the press duration of the click.
     * @param durationMs the new duration in milliseconds.
     */
    fun setPressDuration(durationMs: Long?) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(pressDuration = durationMs))
        }
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(repository, condition, onBitmapLoaded)

    /** Set the condition to click on when the events conditions are fulfilled. */
    fun setConditionToBeClicked(condition: ScreenCondition) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(clickOnConditionId = condition.id))
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            sharedPreferences.edit().putClickPressDurationConfig(click.pressDuration ?: 0).apply()
        }
    }

    fun monitorViews(onConditionTypeView: View, selectPositionFieldView: View, saveButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.CLICK_DIALOG_FIELD_POSITION_TYPE_ITEM_ON_CONDITION, onConditionTypeView)
            attach(MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION, selectPositionFieldView)
            attach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE, saveButton)
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE)
            detach(MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION)
            detach(MonitoredViewType.CLICK_DIALOG_FIELD_POSITION_TYPE_ITEM_ON_CONDITION)
        }
    }

    private fun Context.getUserSelectedClickPositionState(click: Click, forced: Boolean): ClickPositionUiState =
        ClickPositionUiState(
            positionType = Click.PositionType.USER_SELECTED,
            isTypeFieldVisible = !forced,
            isSelectorEnabled = true,
            selectorTitle = getString(R.string.field_click_position_title),
            selectorDescription =
                if (click.position == null) getString(R.string.generic_select_the_position)
                else getString(R.string.field_click_position_desc, click.position?.x ?: 0, click.position?.y ?: 0),
            isClickOffsetEnabled = false,
            isClickOffsetVisible = !forced,
            clickOffsetDescription = getClickOffsetString(click),
        )

    private fun Context.getOnConditionWithOrPositionState(click: Click): ClickPositionUiState =
        ClickPositionUiState(
            positionType = Click.PositionType.ON_DETECTED_CONDITION,
            isTypeFieldVisible = true,
            isSelectorEnabled = false,
            selectorTitle = getString(R.string.field_condition_selection_title_or_operator),
            selectorDescription = getString(R.string.field_condition_selection_desc_or_operator),
            isClickOffsetVisible = true,
            isClickOffsetEnabled = true,
            clickOffsetDescription = getClickOffsetString(click),
        )

    private suspend fun Context.getOnConditionWithAndPositionState(event: ScreenEvent, click: Click): ClickPositionUiState {
        val conditionToClick = event.conditions.find { condition -> click.clickOnConditionId == condition.id }

        return ClickPositionUiState(
            positionType = Click.PositionType.ON_DETECTED_CONDITION,
            isTypeFieldVisible = true,
            isSelectorEnabled = availableConditions.value.isNotEmpty(),
            selectorTitle = getString(R.string.field_condition_selection_title_and_operator),
            selectorDescription =
                if (conditionToClick == null) getString(R.string.field_condition_selection_desc_and_operator_not_found)
                else getString(R.string.field_condition_selection_desc_and_operator, conditionToClick.name),
            selectorConditionState = conditionToClick.toScreenConditionUiState(),
            isClickOffsetVisible = true,
            isClickOffsetEnabled = true,
            clickOffsetDescription = getClickOffsetString(click),
        )
    }

    private fun Context.getClickOffsetString(click: Click): String {
        val offset =
            if (click.positionType == Click.PositionType.ON_DETECTED_CONDITION) click.clickOffset
            else null

        return offset?.let { getString(R.string.field_click_offset_desc, it.x, it.y) }
            ?: getString(R.string.field_click_offset_desc_none)
    }

    private suspend fun ScreenCondition?.toScreenConditionUiState() : ScreenConditionUiState =
        when (this) {
            is ImageCondition -> ScreenConditionUiState.Image(repository.getConditionBitmap(this))
            is TextCondition -> ScreenConditionUiState.Text(textToDetect)
            else -> throw IllegalArgumentException("Invalid screen condition")
        }
}

data class ClickPositionUiState(
    val positionType: Click.PositionType,
    val isTypeFieldVisible: Boolean,
    val isClickOffsetVisible: Boolean,
    val isClickOffsetEnabled: Boolean,
    val clickOffsetDescription: String? = null,
    val isSelectorEnabled: Boolean,
    val selectorTitle: String,
    val selectorDescription: String?,
    val selectorConditionState: ScreenConditionUiState? = null,
)

sealed class ScreenConditionUiState {
    data class Image(val bitmap: Bitmap? = null) : ScreenConditionUiState()
    data class Text(val textToDetect: String) : ScreenConditionUiState()
}

