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

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.condition.GetImageConditionBitmapJobUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getOffsetDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.toPositionUiState
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putClickPressDurationConfig

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.core.content.edit
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class)
class ClickViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getImageConditionBitmapJobUseCase: GetImageConditionBitmapJobUseCase,
    private val displayConfigManager: DisplayConfigManager,
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
            availableConditions = conditionsState.value?.filter { it.shouldBeDetected } ?: emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun getEditedClick(): Click? =
        editionRepository.editionState.getEditedAction<Click>()

    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmapJobUseCase(viewModelScope, condition, onBitmapLoaded)

    /** True if the user must pick the condition to click on, false if it is the first detected one. */
    fun shouldSelectCondition(): Boolean =
        editionRepository.editionState.getEditedEvent()?.conditionOperator == AND

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

    /**
     * Set the position of the click.
     * @param position the new position.
     */
    fun setPosition(position: Point) {
        updateClickPosition(
            x = CoordinatesValue.Literal(position.x),
            y = CoordinatesValue.Literal(position.y),
        )
    }

    /** Set the x coordinate of the click to a literal value. */
    fun setPositionX(newXValue: String) {
        updateClickPosition(x = CoordinatesValue.Literal(newXValue.toIntOrNull() ?: 0))
    }

    /** Set the y coordinate of the click to a literal value. */
    fun setPositionY(newYValue: String) {
        updateClickPosition(y = CoordinatesValue.Literal(newYValue.toIntOrNull() ?: 0))
    }

    /** Set the x coordinate of the click to the value of a counter. */
    fun setPositionXCounter(counterName: String) {
        updateClickPosition(x = CoordinatesValue.Counter(counterName))
    }

    /** Set the y coordinate of the click to the value of a counter. */
    fun setPositionYCounter(counterName: String) {
        updateClickPosition(y = CoordinatesValue.Counter(counterName))
    }

    /** Click on the condition detected by the parent event, whichever it is. */
    fun setClickOnDetectedCondition() {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(
                click.copy(position = Position.Condition(conditionId = null))
            )
        }
    }

    /** Stop clicking on a condition and go back to a click on a position. */
    fun removeClickOnCondition() {
        val displaySize = displayConfigManager.displayConfig.sizePx
        updateClickPosition(
            x = CoordinatesValue.Literal(displaySize.x / 2),
            y = CoordinatesValue.Literal(displaySize.y / 2),
        )
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

    /** The offset to apply to the click position, defaulting to no offset at all. */
    fun getClickOffset(): Position.Coordinates =
        editionRepository.editionState.getEditedAction<Click>()?.offset as? Position.Coordinates
            ?: NO_CLICK_OFFSET

    /** Set the offset to apply to the click position. */
    fun setClickOffset(offset: Position.Coordinates) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(offset = offset))
        }
    }

    /** Set the condition to click on when the events conditions are fulfilled. */
    fun setConditionToBeClicked(condition: ScreenCondition) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(
                click.copy(position = Position.Condition(conditionId = condition.id))
            )
        }
    }

    /**
     * Update the click position, switching it back to a user selected position if it was on a condition.
     * A null coordinate keeps its current value, or defaults to 0 if the click had no coordinates yet.
     */
    private fun updateClickPosition(
        x: CoordinatesValue? = null,
        y: CoordinatesValue? = null,
    ) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            val coordinates = click.position as? Position.Coordinates

            editionRepository.updateEditedAction(
                click.copy(
                    position = Position.Coordinates(
                        x = x ?: coordinates?.x ?: CoordinatesValue.Literal(0),
                        y = y ?: coordinates?.y ?: CoordinatesValue.Literal(0),
                    ),
                )
            )
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            sharedPreferences.edit { putClickPressDurationConfig(click.pressDuration ?: 0) }
        }
    }

    fun monitorViews(selectConditionButton: View, selectPositionButton: View, saveButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.CLICK_DIALOG_BUTTON_CONDITION, selectConditionButton)
            attach(MonitoredViewType.CLICK_DIALOG_BUTTON_POSITION, selectPositionButton)
            attach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE, saveButton)
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE)
            detach(MonitoredViewType.CLICK_DIALOG_BUTTON_POSITION)
            detach(MonitoredViewType.CLICK_DIALOG_BUTTON_CONDITION)
        }
    }

    private fun Click.toDialogUiState(
        context: Context,
        event: Event,
        availableConditions: List<ScreenCondition>,
        hasUnsavedModifications: Boolean,
        canBeSaved: Boolean,
    ) : ClickUiState {
        val positionState = ClickPositionUiState(
            position = toPositionUiState(editionRepository),
            isConditionSelectionVisible = event is ScreenEvent,
            isClickOffsetVisible = event is ScreenEvent,
            clickOffsetDescription = context.getClickOffsetString(this),
        )

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

    private fun Context.getClickOffsetString(click: Click): String {
        return click.offset?.let { getOffsetDescription(it.toPositionUiState(editionRepository)) }
            ?: getString(R.string.field_click_offset_desc_none)
    }
}

private val NO_CLICK_OFFSET = Position.Coordinates(
    x = CoordinatesValue.Literal(0),
    y = CoordinatesValue.Literal(0),
)
