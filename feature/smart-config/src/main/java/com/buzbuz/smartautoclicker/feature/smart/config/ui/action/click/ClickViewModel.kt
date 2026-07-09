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
import android.graphics.Point
import android.view.View
import androidx.core.content.ContextCompat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.ext.getConditionBitmap
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.utils.createColorIndicatorDrawable
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putClickPressDurationConfig

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
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
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class)
class ClickViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val bitmapRepository: BitmapRepository,
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
        .debounce(1000.milliseconds)

    val uiState: StateFlow<ClickUiState?> = combine(
        configuredClick,
        editionRepository.editionState.editedEventState,
        editionRepository.editionState.editedActionState,
        editionRepository.editionState.editedEventScreenConditionsState,
    ) { click, event, actionState, conditionsState ->

        val evt = event.value ?: return@combine null
        click.toDialogUiState(
            context = context,
            event = evt,
            hasUnsavedModifications = actionState.hasChanged,
            canBeSaved = actionState.canBeSaved,
            availableConditions = conditionsState.value
                ?.filter { it.shouldBeDetected }
                ?.map { it.toUiScreenCondition(context = context, shortThreshold = true, inError = !it.isComplete()) }
                ?: emptyList()
            )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun getEditedClick(): Click? =
        editionRepository.editionState.getEditedAction<Click>()

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

    /** Set the condition to click on when the events conditions are fulfilled. */
    fun setConditionToBeClicked(condition: ScreenCondition) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(clickOnConditionId = condition.id))
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            sharedPreferences.edit { putClickPressDurationConfig(click.pressDuration ?: 0) }
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

    private suspend fun Click.toDialogUiState(
        context: Context,
        event: Event,
        availableConditions: List<UiScreenCondition>,
        hasUnsavedModifications: Boolean,
        canBeSaved: Boolean,
    ) : ClickUiState {
        val positionState = when (event) {
            is TriggerEvent ->
                context.getUserSelectedClickPositionState(this, forced = true)

            is ScreenEvent if this.positionType == Click.PositionType.USER_SELECTED ->
                context.getUserSelectedClickPositionState(this, forced = false)

            is ScreenEvent if this.positionType == Click.PositionType.ON_DETECTED_CONDITION && event.conditionOperator == OR ->
                context.getOnConditionWithOrPositionState(this)

            is ScreenEvent if this.positionType == Click.PositionType.ON_DETECTED_CONDITION && event.conditionOperator == AND ->
                context.getOnConditionWithAndPositionState(event, this, availableConditions)

            else -> null
        }

        return ClickUiState(
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasUnsavedModifications,
            name = name,
            nameError = name?.isEmpty() ?: true,
            pressDuration = pressDuration?.toString() ?: "1",
            pressDurationError = (pressDuration ?: -1) <= 0,
            positionState = positionState,
            availableConditions = availableConditions,
        )
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
            isSelectorInError = click.position == null,
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
            isSelectorInError = false,
            isClickOffsetVisible = true,
            isClickOffsetEnabled = true,
            clickOffsetDescription = getClickOffsetString(click),
        )

    private suspend fun Context.getOnConditionWithAndPositionState(
        event: ScreenEvent,
        click: Click,
        availableConditions: List<UiScreenCondition>,
    ): ClickPositionUiState {
        val conditionToClick = event.conditions.find { condition -> click.clickOnConditionId == condition.id }

        val conditionVisualization = when (conditionToClick) {
            is ScreenCondition.Color -> createColorIndicatorDrawable(conditionToClick.color)
            is ScreenCondition.Image -> bitmapRepository.getConditionBitmap(conditionToClick)
            is ScreenCondition.Number -> ContextCompat.getDrawable(this, R.drawable.ic_number_condition)
            is ScreenCondition.Text -> ContextCompat.getDrawable(this, R.drawable.ic_text_condition)
            null -> null
        }

        return ClickPositionUiState(
            positionType = Click.PositionType.ON_DETECTED_CONDITION,
            isTypeFieldVisible = true,
            isSelectorEnabled = availableConditions.isNotEmpty(),
            selectorTitle = getString(R.string.field_condition_selection_title_and_operator),
            selectorDescription =
                if (conditionToClick == null || conditionVisualization == null) getString(R.string.field_condition_selection_desc_and_operator_not_found)
                else getString(R.string.field_condition_selection_desc_and_operator, conditionToClick.name),
            selectorVisualization = conditionVisualization,
            isSelectorInError = availableConditions.isNotEmpty() && (conditionToClick == null || conditionVisualization == null),
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
}
