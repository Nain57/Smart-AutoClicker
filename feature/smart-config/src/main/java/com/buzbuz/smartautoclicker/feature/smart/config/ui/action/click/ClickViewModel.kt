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
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiImageCondition
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
        .filterIsInstance<Action.Click>()

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

    val availableConditions: StateFlow<List<UiImageCondition>> = editionRepository.editionState.editedEventImageConditionsState
        .map { editedConditions ->
            editedConditions.value?.filter { it.shouldBeDetected }
                ?.map {
                    it.toUiImageCondition(
                        context = context,
                        shortThreshold = true,
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

                evt is ImageEvent && click.positionType == Action.Click.PositionType.USER_SELECTED ->
                    context.getUserSelectedClickPositionState(click, forced = false)

                evt is ImageEvent && click.positionType == Action.Click.PositionType.ON_DETECTED_CONDITION && event.value.conditionOperator == OR ->
                    context.getOnConditionWithOrPositionState()

                evt is ImageEvent && click.positionType == Action.Click.PositionType.ON_DETECTED_CONDITION && event.value.conditionOperator == AND ->
                    context.getOnConditionWithAndPositionState(evt, click)

                else -> null
            }
        }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /** Tells if the configured click is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    fun getEditedClick(): Action.Click? =
        editionRepository.editionState.getEditedAction()

    /**
     * Set the name of the click.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(name = "" + name))
        }
    }

    /** Set if this click should be made on the detected condition. */
    fun setClickOnCondition(newType: Action.Click.PositionType) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(positionType = newType))
        }
    }

    /**
     * Set the position of the click.
     * @param position the new position.
     */
    fun setPosition(position: Point) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(x = position.x, y = position.y))
        }
    }

    /**
     * Set the press duration of the click.
     * @param durationMs the new duration in milliseconds.
     */
    fun setPressDuration(durationMs: Long?) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
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
    fun setConditionToBeClicked(condition: ImageCondition) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(clickOnConditionId = condition.id))
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
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

    private fun Context.getUserSelectedClickPositionState(click: Action.Click, forced: Boolean): ClickPositionUiState =
        ClickPositionUiState(
            positionType = Action.Click.PositionType.USER_SELECTED,
            isTypeFieldVisible = !forced,
            isSelectorEnabled = true,
            selectorTitle = getString(R.string.field_click_position_title),
            selectorDescription =
                if (click.x == null || click.y == null) getString(R.string.generic_select_the_position)
                else getString(R.string.field_click_position_desc, click.x!!, click.y!!),
        )

    private fun Context.getOnConditionWithOrPositionState(): ClickPositionUiState =
        ClickPositionUiState(
            positionType = Action.Click.PositionType.ON_DETECTED_CONDITION,
            isTypeFieldVisible = true,
            isSelectorEnabled = false,
            selectorTitle = getString(R.string.field_condition_selection_title_or_operator),
            selectorDescription = getString(R.string.field_condition_selection_desc_or_operator),
        )

    private suspend fun Context.getOnConditionWithAndPositionState(event: ImageEvent, click: Action.Click): ClickPositionUiState {
        val conditionToClick = event.conditions.find { condition -> click.clickOnConditionId == condition.id }
        val conditionBitmap = conditionToClick?.let { condition -> repository.getConditionBitmap(condition) }

        return ClickPositionUiState(
            positionType = Action.Click.PositionType.ON_DETECTED_CONDITION,
            isTypeFieldVisible = true,
            isSelectorEnabled = availableConditions.value.isNotEmpty(),
            selectorTitle = getString(R.string.field_condition_selection_title_and_operator),
            selectorDescription =
                if (conditionToClick == null || conditionBitmap == null) getString(R.string.field_condition_selection_desc_and_operator_not_found)
                else getString(R.string.field_condition_selection_desc_and_operator, conditionToClick.name),
            selectorBitmap = conditionBitmap,
        )
    }
}

data class ClickPositionUiState(
    val positionType: Action.Click.PositionType,
    val isTypeFieldVisible: Boolean,
    val isSelectorEnabled: Boolean,
    val selectorTitle: String,
    val selectorDescription: String?,
    val selectorBitmap: Bitmap? = null,
)