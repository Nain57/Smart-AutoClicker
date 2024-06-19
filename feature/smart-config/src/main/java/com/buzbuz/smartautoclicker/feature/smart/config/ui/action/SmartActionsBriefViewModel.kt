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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action

import android.content.Context
import android.graphics.PointF
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.PauseDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.SwipeDescription
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.actions.ActionTypeChoice
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.Collections
import javax.inject.Inject

class SmartActionsBriefViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val editedActions: Flow<EditedListState<Action>> = editionRepository.editionState.editedEventActionsState

    private val _isGestureCaptureStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isGestureCaptureStarted: StateFlow<Boolean> = _isGestureCaptureStarted

    val actionBriefList: Flow<List<SmartActionBriefItem>> = editedActions.map { actions ->
        val actionList = actions.value ?: emptyList()
        actionList.mapIndexed { index, action -> action.toActionBrief(context, !actions.itemValidity[index]) }
    }

    private val currentFocusActionIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private val focusedAction: Flow<Action?> =
        combine(currentFocusActionIndex, editedActions) { focusedIndex, actions ->
            val actionList = actions.value ?: return@combine null
            if (focusedIndex !in actionList.indices) return@combine null
            actionList[focusedIndex]
        }

    val actionVisualization : Flow<ActionDescription?> =
        combine(focusedAction, _isGestureCaptureStarted) { action, isCapturing ->
            action?.toActionDescription() to isCapturing
        }.filter { (_, isCapturing) -> !isCapturing }.map { it.first }

    fun startGestureCaptureState() {
        _isGestureCaptureStarted.value = true
    }

    fun endGestureCaptureState(context: Context, gesture: ActionDescription) {
        val action = gesture.toAction(context) ?: return
        editionRepository.apply {
            startActionEdition(action)
            upsertEditedAction()
        }

        _isGestureCaptureStarted.value = false
    }

    fun cancelGestureCaptureState() {
        _isGestureCaptureStarted.value = false
    }

    fun setFocusedActionIndex(index: Int) {
        currentFocusActionIndex.value = index
    }

    fun createAction(context: Context, actionType: ActionTypeChoice): Action = when (actionType) {
        is ActionTypeChoice.Click -> editionRepository.editedItemsBuilder.createNewClick(context)
        is ActionTypeChoice.Swipe -> editionRepository.editedItemsBuilder.createNewSwipe(context)
        is ActionTypeChoice.Pause -> editionRepository.editedItemsBuilder.createNewPause(context)
        is ActionTypeChoice.Intent -> editionRepository.editedItemsBuilder.createNewIntent(context)
        is ActionTypeChoice.ToggleEvent -> editionRepository.editedItemsBuilder.createNewToggleEvent(context)
        is ActionTypeChoice.ChangeCounter -> editionRepository.editedItemsBuilder.createNewChangeCounter(context)
    }

    fun createNewActionFrom(action: Action): Action =
        editionRepository.editedItemsBuilder.createNewActionFrom(action)

    fun startActionEdition(action: Action) = editionRepository.startActionEdition(action)

    fun upsertEditedAction() = editionRepository.upsertEditedAction()

    fun removeEditedAction() = editionRepository.deleteEditedAction()

    fun dismissEditedAction() = editionRepository.stopActionEdition()

    fun upsertActionFromGesture(context: Context, gesture: ActionDescription) {

    }

    fun playAction(index: Int) {

    }

    fun moveAction(from: Int, to: Int) {
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        Collections.swap(actions, from, to)

        editionRepository.updateActionsOrder(actions)
    }

    fun deleteAction(index: Int) {
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        if (index !in actions.indices) return

        editionRepository.apply {
            startActionEdition(actions[index])
            deleteEditedAction()
        }
    }

    private fun ActionDescription.toAction(context: Context): Action? =
        when (this) {
            is ClickDescription -> editionRepository.editedItemsBuilder.createNewClick(context)
                .copy(
                    x = position?.x?.toInt() ?: 0,
                    y =  position?.y?.toInt() ?: 0,
                    pressDuration = pressDurationMs,
                    positionType = Action.Click.PositionType.USER_SELECTED,
                )

            is SwipeDescription -> editionRepository.editedItemsBuilder.createNewSwipe(context)
                .copy(
                    fromX = from?.x?.toInt() ?: 0,
                    fromY =  from?.y?.toInt() ?: 0,
                    toX = to?.x?.toInt() ?: 0,
                    toY =  to?.y?.toInt() ?: 0,
                    swipeDuration = swipeDurationMs,
                )

            else -> null
        }

    private fun Action.toActionDescription(): ActionDescription? = when (this) {
        is Action.Click -> ClickDescription(
            position = PointF((x ?: 0).toFloat(), (y ?: 0).toFloat()),
            pressDurationMs = pressDuration ?: 1,
        )

        is Action.Swipe -> SwipeDescription(
            from = PointF((fromX ?: 0).toFloat(), (fromY ?: 0).toFloat()),
            to = PointF((toX ?: 0).toFloat(), (toY ?: 0).toFloat()),
            swipeDurationMs = swipeDuration ?: 1,
        )

        is Action.Pause -> PauseDescription(
            pauseDurationMs = pauseDuration ?: 1,
        )

        is Action.ChangeCounter,
        is Action.Intent,
        is Action.ToggleEvent -> null
    }
}
