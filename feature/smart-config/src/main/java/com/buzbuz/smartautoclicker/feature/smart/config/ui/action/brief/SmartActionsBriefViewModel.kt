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
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.ext.getConditionBitmap
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger as DomainGestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion as DomainGestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.ui.utils.createColorIndicatorDrawable
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGesture
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFingerMotion
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.DefaultDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.PauseDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.availability.IsActionCopyAvailableUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeChoice
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.toItemBriefs
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper.toGestureDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper.toWaypointConditions

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.util.Collections
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


class SmartActionsBriefViewModel @Inject constructor(
    @ApplicationContext context: Context,
    isActionCopyAvailableUseCase: IsActionCopyAvailableUseCase,
    private val bitmapRepository: BitmapRepository,
    private val editionRepository: EditionRepository,
    private val displayConfigManager: DisplayConfigManager,
    private val smartProcessingRepository: SmartProcessingRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
    tutorialRepository: TutorialRepository,
) : ViewModel(), ActionConfigurator {

    private val editedActions: Flow<EditedListState<Action>> = editionRepository.editionState.editedEventActionsState
    private val editedEvent: Flow<Event> = editionRepository.editionState.editedEventState.mapNotNull { it.value }

    private val visualizationState: MutableStateFlow<BriefVisualizationState> =
        MutableStateFlow(BriefVisualizationState(focusedIndex = 0, isGestureCaptureStarted = false))

    private val actionBriefs: Flow<List<ItemBrief>> =
        combine(editedEvent, editedActions) { event, actions -> actions.toItemBriefs(context, event) }

    private val focusedActionBrief: Flow<FocusedActionBrief> =
        combine(visualizationState, editedActions) { state, actions ->
            val focusedAction = actions.value?.getOrNull(state.focusedIndex)
            if (state.isRecordedGestureDisplayed(focusedAction)) null to true
            else focusedAction to false
        }
            .distinctUntilChanged()
            .map { (action, isRecordedGestureDisplayed) ->
                FocusedActionBrief(
                    description = action?.toActionDescription(context),
                    isRecordedGestureDisplayed = isRecordedGestureDisplayed,
                )
            }

    private val mode: Flow<SmartActionsBriefMode> = combine(
        visualizationState,
        smartProcessingRepository.detectionState,
    ) { state, detectionState ->
        when {
            detectionState == DetectionState.DETECTING -> SmartActionsBriefMode.ACTION_TESTING
            state.isGestureCaptureStarted -> SmartActionsBriefMode.GESTURE_CAPTURE
            else -> SmartActionsBriefMode.IDLE
        }
    }

    private val isTutorialModeEnabled: Flow<Boolean> =
        tutorialRepository.tutorialState.map { it is TutorialState.Started }

    val uiState: StateFlow<SmartActionsBriefUiState?> = combine(
        actionBriefs,
        focusedActionBrief,
        mode,
        isTutorialModeEnabled,
    ) { briefs, focusedBrief, mode, isTutorialEnabled ->
        SmartActionsBriefUiState(
            actionBriefs = briefs,
            focusedActionDescription = focusedBrief.description,
            isRecordedGestureDisplayed = focusedBrief.isRecordedGestureDisplayed,
            mode = mode,
            isBriefPanelAutoHideEnabled = !isTutorialEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val actionTypeChoices: StateFlow<List<ActionTypeChoice>> =
        isActionCopyAvailableUseCase().map { canCopy ->
            buildList {
                if (canCopy) add(ActionTypeChoice.Copy)
                add(ActionTypeChoice.Click)
                add(ActionTypeChoice.Gesture)
                add(ActionTypeChoice.Pause)
                add(ActionTypeChoice.SetText)
                add(ActionTypeChoice.System)
                add(ActionTypeChoice.ChangeCounter)
                add(ActionTypeChoice.ToggleEvent)
                add(ActionTypeChoice.Notification)
                add(ActionTypeChoice.Intent)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun isGestureCaptureStarted(): Boolean =
        visualizationState.value.isGestureCaptureStarted

    fun startGestureCapture() {
        visualizationState.update { state ->
            state.copy(isGestureCaptureStarted = true, pendingRecordedActionId = null)
        }
    }

    /** Ends the capture, adding the recorded gesture as a new action if any was actually captured. */
    fun endGestureCapture(context: Context, gesture: RecordedGesture?) {
        val action = gesture?.toAction(context)
        visualizationState.update { state ->
            state.copy(isGestureCaptureStarted = false, pendingRecordedActionId = action?.id)
        }

        action ?: return
        editionRepository.apply {
            startActionEdition(action)
            upsertEditedAction()
        }
    }

    fun cancelGestureCapture() {
        visualizationState.update { state ->
            state.copy(isGestureCaptureStarted = false, pendingRecordedActionId = null)
        }
    }

    fun setFocusedActionIndex(index: Int) {
        val actions = editionRepository.editionState.getEditedEventActions<Action>()
        visualizationState.update { state ->
            val isRecordedActionInserted = actions?.any { it.id == state.pendingRecordedActionId } == true
            state.copy(
                focusedIndex = index,
                pendingRecordedActionId = if (isRecordedActionInserted) null else state.pendingRecordedActionId,
            )
        }
    }

    override fun getActionTypeChoices(): List<ActionTypeChoice> =
        actionTypeChoices.value

    override fun createAction(context: Context, choice: ActionTypeChoice): Action =
        editionRepository.createNewAction(context, choice)

    override fun startActionEdition(action: Action) {
        editionRepository.startActionEdition(action)
    }

    override fun upsertEditedAction() {
        editionRepository.upsertEditedAction()
    }

    override fun removeEditedAction() {
        editionRepository.deleteEditedAction()
    }

    override fun dismissEditedAction() {
        editionRepository.stopActionEdition()
    }

    fun playAction(context: Context, index: Int) {
        val scenario = editionRepository.editionState.getScenario()
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList()
        if (scenario == null || actions == null || index !in actions.indices) return

        viewModelScope.launch {
            delay(500.milliseconds)
            smartProcessingRepository.tryAction(context, scenario, actions[index])
        }
    }

    fun stopAction(): Boolean {
        if (!smartProcessingRepository.isRunning()) return false

        smartProcessingRepository.stopDetection()
        return true
    }

    fun swapActions(i: Int, j: Int) {
        if (i == j) return

        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        if (actions.size <= 1 || i !in actions.indices || j !in actions.indices) return

        Collections.swap(actions, i, j)
        editionRepository.updateActionsOrder(actions)
    }

    fun moveAction(from: Int, to: Int) {
        if (from == to) return

        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        if (actions.size <= 1 || from !in actions.indices || to !in actions.indices) return

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

    private fun RecordedGesture.toAction(context: Context): Action =
        when (this) {
            is RecordedGesture.Click -> {
                val pt = position.toPoint()
                editionRepository.editedItemsBuilder.createNewClick(context)
                    .copy(
                        position = Position.Coordinates(
                            x = CoordinatesValue.Literal(pt.x),
                            y = CoordinatesValue.Literal(pt.y),
                        ),
                        pressDuration = durationMs,
                    )
            }

            is RecordedGesture.Gesture -> {
                val builder = editionRepository.editedItemsBuilder
                val gestureStartMs = fingers.minOfOrNull { it.pressedAtMs } ?: 0L
                builder.createNewGesture(context).copy(
                    fingers = fingers.map { uiFinger ->
                        DomainGestureFinger(
                            id = builder.newFingerIdentifier(),
                            startTimeMs = uiFinger.pressedAtMs - gestureStartMs,
                            motions = uiFinger.motions.map { uiMotion ->
                                when (uiMotion) {
                                    is RecordedGestureFingerMotion.Press -> {
                                        val pt = uiMotion.position.toPoint()
                                        DomainGestureFingerMotion.Press(
                                            id = builder.newMotionIdentifier(),
                                            position = Position.Coordinates(
                                                x = CoordinatesValue.Literal(pt.x),
                                                y = CoordinatesValue.Literal(pt.y),
                                            ),
                                            offset = Position.Coordinates(
                                                x = CoordinatesValue.Literal(0),
                                                y = CoordinatesValue.Literal(0),
                                            ),
                                        )
                                    }
                                    is RecordedGestureFingerMotion.Move -> {
                                        val pt = uiMotion.moveToPosition.toPoint()
                                        DomainGestureFingerMotion.Move(
                                            id = builder.newMotionIdentifier(),
                                            position = Position.Coordinates(
                                                x = CoordinatesValue.Literal(pt.x),
                                                y = CoordinatesValue.Literal(pt.y),
                                            ),
                                            offset = Position.Coordinates(
                                                x = CoordinatesValue.Literal(0),
                                                y = CoordinatesValue.Literal(0),
                                            ),
                                            durationMs = uiMotion.moveToDurationMs,
                                        )
                                    }
                                    is RecordedGestureFingerMotion.Wait -> DomainGestureFingerMotion.Wait(
                                        id = builder.newMotionIdentifier(),
                                        durationMs = uiMotion.waitDurationMs,
                                    )
                                    RecordedGestureFingerMotion.Release -> DomainGestureFingerMotion.Release(
                                        id = builder.newMotionIdentifier(),
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }

    private suspend fun Action.toActionDescription(context: Context): ItemBriefDescription = when (this) {
        is Click -> ClickDescription(
            position = getLiteralPosition()?.toPointF(),
            pressDurationMs = pressDuration ?: 1,
            imageConditionBitmap = findClickOnConditionBitmap(context),
        )

        is Gesture -> {
            val conditions = editionRepository.editionState.getEditedEventScreenConditions() ?: emptyList()
            toGestureDescription(
                conditionOperator = editionRepository.editionState.getEditedEvent()?.conditionOperator ?: AND,
                conditions = conditions,
                counters = editionRepository.editionState.getAllEditedCounters(),
                screenSize = displayConfigManager.displayConfig.sizePx,
                conditionDisplays = conditions.toWaypointConditions(context, bitmapRepository),
            )
        }

        is Pause -> PauseDescription(
            pauseDurationMs = pauseDuration ?: 1,
        )

        else -> DefaultDescription(
            icon = ContextCompat.getDrawable(context, getIconRes())
        )
    }

    private suspend fun Click.findClickOnConditionBitmap(context: Context): Bitmap? {
        val conditionId = getClickOnConditionId() ?: return null

        return editionRepository.editionState.getEditedEventScreenConditions()
            ?.find { it.id == conditionId }
            ?.let { condition ->
                when (condition) {
                    is ScreenCondition.Color -> context.createColorIndicatorDrawable(condition.color)?.toBitmap()
                    is ScreenCondition.Image -> bitmapRepository.getConditionBitmap(condition)
                    is ScreenCondition.Number -> ContextCompat.getDrawable(context, R.drawable.ic_number_condition)?.toBitmap()
                    is ScreenCondition.Text -> ContextCompat.getDrawable(context, R.drawable.ic_text_condition)?.toBitmap()
                }
            }
    }
}

private data class BriefVisualizationState(
    val focusedIndex: Int,
    val isGestureCaptureStarted: Boolean,
    /** Action created from the last recorded gesture, until the brief list focuses it. */
    val pendingRecordedActionId: Identifier? = null,
) {

    /**
     * The brief view keeps the recorded gesture description until its action is focused, otherwise the previously
     * focused action would flash while the list inserts and scrolls to the new one.
     */
    fun isRecordedGestureDisplayed(focusedAction: Action?): Boolean =
        isGestureCaptureStarted ||
                (pendingRecordedActionId != null && pendingRecordedActionId != focusedAction?.id)
}

private data class FocusedActionBrief(
    val description: ItemBriefDescription?,
    val isRecordedGestureDisplayed: Boolean,
)