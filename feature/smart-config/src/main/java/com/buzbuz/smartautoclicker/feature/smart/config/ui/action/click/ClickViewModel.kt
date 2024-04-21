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
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
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
    val nameError: Flow<Boolean> = configuredClick.map { it.name?.isEmpty() ?: true }

    /** The duration between the press and release of the click in milliseconds. */
    val pressDuration: Flow<String?> = configuredClick
        .map { it.pressDuration?.toString() }
        .take(1)
    /** Tells if the press duration value is valid or not. */
    val pressDurationError: Flow<Boolean> = configuredClick.map { (it.pressDuration ?: -1) <= 0 }

    val availableConditions: StateFlow<List<ImageCondition>> = editionRepository.editionState.editedEventImageConditionsState
        .map { editedConditions -> editedConditions.value?.filter { it.shouldBeDetected } ?: emptyList() }
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

    val clickTypeItemOnCondition = DropdownItem(
        title = R.string.dropdown_item_title_click_position_type_on_condition,
        helperText = R.string.dropdown_helper_text_click_position_type_on_condition,
    )
    private val clickTypeItemOnPosition = DropdownItem(
        title= R.string.dropdown_item_title_click_position_type_on_position,
        helperText = R.string.dropdown_helper_text_click_position_type_on_position,
    )
    /** Items for the click type dropdown field. */
    val clickTypeItems = listOf(clickTypeItemOnCondition, clickTypeItemOnPosition)

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
    fun setClickOnCondition(newItem: DropdownItem) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            val positionType = when (newItem) {
                clickTypeItemOnCondition -> Action.Click.PositionType.ON_DETECTED_CONDITION
                clickTypeItemOnPosition -> Action.Click.PositionType.USER_SELECTED
                else -> return
            }

            editionRepository.updateEditedAction(click.copy(positionType = positionType))
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

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE, view)
    }

    fun monitorSelectPositionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CLICK_DIALOG_BUTTON_SELECT_POSITION_OR_CONDITION, view)
    }

    fun monitorClickOnDropdownView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_CLICK_ON, view)
    }

    fun monitorDropdownItemConditionView(view: View) {
        monitoredViewsManager.attach(
            MonitoredViewType.CLICK_DIALOG_DROPDOWN_ITEM_CLICK_ON_CONDITION,
            view,
            ViewPositioningType.SCREEN,
        )
    }

    fun stopDropdownItemConditionViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_ITEM_CLICK_ON_CONDITION)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE)
            detach(MonitoredViewType.CLICK_DIALOG_BUTTON_SELECT_POSITION_OR_CONDITION)
            detach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_ITEM_CLICK_ON_CONDITION)
            detach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_CLICK_ON)
        }
    }

    private fun Context.getUserSelectedClickPositionState(click: Action.Click, forced: Boolean): ClickPositionUiState {
        val positionText =
            if (click.x == null || click.y == null) getString(R.string.item_desc_position_select)
            else getString(R.string.item_desc_click_on_position, click.x!!, click.y!!)

        return ClickPositionUiState(
            selectedChoice = clickTypeItemOnPosition,
            selectorTitle = getString(R.string.item_title_click_position),
            selectorSubText = positionText,
            selectorIcon = null,
            chevronIsVisible = true,
            action = ClickPositionSelectorAction.SELECT_POSITION,
            forTriggerEvent = forced,
        )
    }

    private fun Context.getOnConditionWithOrPositionState(): ClickPositionUiState =
        ClickPositionUiState(
            selectedChoice = clickTypeItemOnCondition,
            selectorTitle = getString(R.string.item_title_click_on_condition_or_operator),
            selectorSubText = getString(R.string.item_desc_click_on_condition_or_operator),
            selectorIcon = null,
            chevronIsVisible = false,
            action = ClickPositionSelectorAction.NONE,
            forTriggerEvent = false,
        )

    private suspend fun Context.getOnConditionWithAndPositionState(event: ImageEvent, click: Action.Click): ClickPositionUiState {
        val conditionToClick = event.conditions.find { condition -> click.clickOnConditionId == condition.id }
        val conditionBitmap = conditionToClick?.let { condition -> repository.getConditionBitmap(condition) }

        val subText: String =
            if (conditionToClick == null || conditionBitmap == null) getString(R.string.item_desc_click_on_condition_and_operator_not_found)
            else getString(R.string.item_desc_click_on_condition_and_operator, conditionToClick.name)

        val atLeastOneCondition = availableConditions.value.isNotEmpty()

        return ClickPositionUiState(
            selectedChoice = clickTypeItemOnCondition,
            selectorTitle = getString(R.string.item_title_click_on_condition_and_operator),
            selectorSubText = subText,
            selectorIcon = conditionBitmap,
            chevronIsVisible = atLeastOneCondition,
            action = if (atLeastOneCondition) ClickPositionSelectorAction.SELECT_CONDITION else ClickPositionSelectorAction.NONE,
            forTriggerEvent = false,
        )
    }
}

data class ClickPositionUiState(
    val selectedChoice: DropdownItem,
    val selectorTitle: String,
    val selectorSubText: String?,
    val selectorIcon: Bitmap?,
    val chevronIsVisible: Boolean,
    val action: ClickPositionSelectorAction,
    val forTriggerEvent: Boolean,
)

enum class ClickPositionSelectorAction {
    NONE,
    SELECT_POSITION,
    SELECT_CONDITION,
}