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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.DefaultDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.PauseDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.SwipeDescription
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeChoice
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getChangeCounterIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIntentIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getToggleEventIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.toUiAction

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.util.Collections
import javax.inject.Inject


class SmartActionsBriefViewModel @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
    private val repository: IRepository,
    private val editionRepository: EditionRepository,
    private val detectionRepository: DetectionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    private val editedActions: Flow<EditedListState<Action>> = editionRepository.editionState.editedEventActionsState
    private val editedEvent: Flow<Event> = editionRepository.editionState.editedEventState.mapNotNull { it.value }

    private val briefVisualizationState: MutableStateFlow<BriefVisualizationState> =
        MutableStateFlow(BriefVisualizationState(0, false))

    val isGestureCaptureStarted: StateFlow<Boolean> = briefVisualizationState
        .map { it.gestureCaptureStarted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val actionBriefList: Flow<List<ItemBrief>> =
        combine(editedEvent, editedActions) { event, actions ->
            val actionList = actions.value ?: emptyList()
            actionList.mapIndexed { index, action ->
                ItemBrief(action.id, action.toUiAction(context, event, inError = !actions.itemValidity[index]) )
            }
        }

    private val focusedAction: Flow<Pair<Action?, Boolean>> =
        combine(briefVisualizationState, editedActions) { visualizationState, actions ->
            val filterUpdates = visualizationState.gestureCaptureStarted
            val actionList = actions.value ?: return@combine null to filterUpdates
            if (visualizationState.focusedIndex !in actionList.indices) return@combine null to filterUpdates
            actionList[visualizationState.focusedIndex] to filterUpdates
        }

    val actionVisualization: Flow<ItemBriefDescription?> = focusedAction
        .filter { !it.second }
        .map { (action, _) -> action?.toActionDescription(context) }

    val actionTypeChoices: StateFlow<List<ActionTypeChoice>> =
        editionRepository.editionState.canCopyActions.map { canCopy ->
            buildList {
                if (canCopy) add(ActionTypeChoice.Copy)
                add(ActionTypeChoice.Click)
                add(ActionTypeChoice.Swipe)
                add(ActionTypeChoice.Pause)
                add(ActionTypeChoice.ChangeCounter)
                add(ActionTypeChoice.ToggleEvent)
                add(ActionTypeChoice.Intent)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isTutorialModeEnabled: Flow<Boolean> =
        repository.isTutorialModeEnabledFlow()

    fun startGestureCaptureState() {
        briefVisualizationState.value = briefVisualizationState.value
            .copy(gestureCaptureStarted = true)
    }

    fun endGestureCaptureState(context: Context, gesture: ItemBriefDescription) {
        val action = gesture.toAction(context) ?: return
        editionRepository.apply {
            startActionEdition(action)
            upsertEditedAction()
        }
    }

    fun cancelGestureCaptureState() {
        briefVisualizationState.value = briefVisualizationState.value
            .copy(gestureCaptureStarted = false)
    }

    fun setFocusedActionIndex(index: Int) {
        briefVisualizationState.value = BriefVisualizationState(
            focusedIndex = index,
            gestureCaptureStarted = false,
        )
    }

    fun createAction(context: Context, actionType: ActionTypeChoice): Action = when (actionType) {
        ActionTypeChoice.Click -> editionRepository.editedItemsBuilder.createNewClick(context)
        ActionTypeChoice.Swipe -> editionRepository.editedItemsBuilder.createNewSwipe(context)
        ActionTypeChoice.Pause -> editionRepository.editedItemsBuilder.createNewPause(context)
        ActionTypeChoice.Intent -> editionRepository.editedItemsBuilder.createNewIntent(context)
        ActionTypeChoice.ToggleEvent -> editionRepository.editedItemsBuilder.createNewToggleEvent(context)
        ActionTypeChoice.ChangeCounter -> editionRepository.editedItemsBuilder.createNewChangeCounter(context)
        ActionTypeChoice.Copy -> throw IllegalArgumentException("Unsupported action type for creation $actionType")
    }

    fun createNewActionFrom(action: Action): Action =
        editionRepository.editedItemsBuilder.createNewActionFrom(action)

    fun startActionEdition(action: Action) = editionRepository.startActionEdition(action)

    fun upsertEditedAction() = editionRepository.upsertEditedAction()

    fun removeEditedAction() = editionRepository.deleteEditedAction()

    fun dismissEditedAction() = editionRepository.stopActionEdition()

    fun playAction(context: Context, index: Int, onCompleted: () -> Unit) {
        val scenario = editionRepository.editionState.getScenario()
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList()
        if (scenario == null || actions == null || index !in actions.indices) return

        viewModelScope.launch {
            delay(500)
            detectionRepository.tryAction(context, scenario, actions[index]) {
                viewModelScope.launch(mainDispatcher) { onCompleted() }
            }
        }
    }

    fun swapActions(i: Int, j: Int) {
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        Collections.swap(actions, i, j)

        editionRepository.updateActionsOrder(actions)
    }

    fun moveAction(from: Int, to: Int) {
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        val movedAction = actions.removeAt(from)
        actions.add(to, movedAction)

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

    fun monitorBriefFirstItemView(briefItemView: View) {
        monitoredViewsManager.attach(
            MonitoredViewType.ACTIONS_BRIEF_FIRST_ITEM,
            briefItemView,
            ViewPositioningType.SCREEN,
        )
    }

    fun monitorViews(createMenuButton: View, saveMenuButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION, createMenuButton, ViewPositioningType.SCREEN)
            attach(MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE, saveMenuButton, ViewPositioningType.SCREEN)
        }
    }

    fun stopBriefFirstItemMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.ACTIONS_BRIEF_FIRST_ITEM)
    }

    fun stopAllViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.ACTIONS_BRIEF_FIRST_ITEM)
            detach(MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION)
            detach(MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE)
        }
    }

    private fun ItemBriefDescription.toAction(context: Context): Action? =
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

    private suspend fun Action.toActionDescription(context: Context): ItemBriefDescription = when (this) {
        is Action.Click -> ClickDescription(
            position = PointF((x ?: 0).toFloat(), (y ?: 0).toFloat()),
            pressDurationMs = pressDuration ?: 1,
            imageConditionBitmap = findClickOnConditionBitmap(),
        )

        is Action.Swipe -> SwipeDescription(
            from = PointF((fromX ?: 0).toFloat(), (fromY ?: 0).toFloat()),
            to = PointF((toX ?: 0).toFloat(), (toY ?: 0).toFloat()),
            swipeDurationMs = swipeDuration ?: 1,
        )

        is Action.Pause -> PauseDescription(
            pauseDurationMs = pauseDuration ?: 1,
        )

        is Action.ChangeCounter -> DefaultDescription(
            ContextCompat.getDrawable(context, getChangeCounterIconRes())
        )

        is Action.Intent -> DefaultDescription(
            ContextCompat.getDrawable(context, getIntentIconRes())
        )

        is Action.ToggleEvent -> DefaultDescription(
            ContextCompat.getDrawable(context, getToggleEventIconRes())
        )
    }

    private suspend fun Action.Click.findClickOnConditionBitmap(): Bitmap? {
        if (positionType != Action.Click.PositionType.ON_DETECTED_CONDITION) return null

        return editionRepository.editionState.getEditedEventConditions<ImageCondition>()
            ?.find { it.id == clickOnConditionId }
            ?.let { repository.getConditionBitmap(it) }
    }
}

private data class BriefVisualizationState(
    val focusedIndex: Int,
    val gestureCaptureStarted: Boolean,
)