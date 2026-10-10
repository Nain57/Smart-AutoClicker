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
package com.buzbuz.smartautoclicker.feature.smart.config.domain

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.identifier.IdentifierCreator
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.bitmaps.CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Move
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Press
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Release
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Wait
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.data.ScenarioEditor

class EditedItemsBuilder internal constructor(
    private val displayConfigManager: DisplayConfigManager,
    private val bitmapRepository: BitmapRepository,
    private val editor: ScenarioEditor,
) {

    private val defaultValues = EditionDefaultValues()
    private val eventsIdCreator = IdentifierCreator()
    private val conditionsIdCreator = IdentifierCreator()
    private val actionsIdCreator = IdentifierCreator()
    private val intentExtrasIdCreator = IdentifierCreator()
    private val eventTogglesIdCreator = IdentifierCreator()
    private val endConditionsIdCreator = IdentifierCreator()
    private val gestureFingerIdCreator = IdentifierCreator()
    private val gestureMotionIdCreator = IdentifierCreator()

    /**
     * Map of original condition list ids to copy condition ids.
     * Will contain data only when creating an event from another one.
     */
    private val eventCopyConditionIdMap =  mutableMapOf<Identifier, Identifier>()

    /** Keep track of new images created during the edition session. */
    private val _newImageConditionsPaths: MutableList<String> = mutableListOf()
    internal val newImageConditionsPaths: List<String> = _newImageConditionsPaths

    internal fun resetBuilder() {
        eventsIdCreator.resetIdCount()
        conditionsIdCreator.resetIdCount()
        actionsIdCreator.resetIdCount()
        intentExtrasIdCreator.resetIdCount()
        endConditionsIdCreator.resetIdCount()
        gestureFingerIdCreator.resetIdCount()
        gestureMotionIdCreator.resetIdCount()
        eventCopyConditionIdMap.clear()
        _newImageConditionsPaths.clear()
    }

    fun createNewImageEvent(context: Context): ScreenEvent =
        ScreenEvent(
            id = eventsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = defaultValues.eventName(context),
            conditionOperator = defaultValues.eventConditionOperator(),
            priority = getEditedImageEventsCountOrThrow(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
            keepDetecting = false,
            cooldownMs = 0L,
        )

    fun createNewTriggerEvent(context: Context): TriggerEvent =
        TriggerEvent(
            id = eventsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = defaultValues.eventName(context),
            conditionOperator = defaultValues.eventConditionOperator(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
        )

    fun createNewImageEventFrom(from: ScreenEvent, scenarioId: Identifier = getEditedScenarioIdOrThrow()): ScreenEvent {
        val eventId = eventsIdCreator.generateNewIdentifier()

        return from.copy(
            id = eventId,
            scenarioId = scenarioId,
            name = "" + from.name,
            conditions = from.conditions.map { conditionOrig ->
                val conditionCopy = createNewScreenConditionFrom(conditionOrig, eventId)
                eventCopyConditionIdMap[conditionOrig.id] = conditionCopy.id
                conditionCopy
            },
            actions = from.actions.map { createNewActionFrom(it, eventId) }
        ).also { eventCopyConditionIdMap.clear() }
    }

    fun createNewTriggerEventFrom(from: TriggerEvent, scenarioId: Identifier = getEditedScenarioIdOrThrow()): TriggerEvent {
        val eventId = eventsIdCreator.generateNewIdentifier()

        return from.copy(
            id = eventId,
            scenarioId = scenarioId,
            name = "" + from.name,
            conditions = from.conditions.map { conditionOrig ->
                val conditionCopy = createNewTriggerConditionFrom(conditionOrig, eventId)
                eventCopyConditionIdMap[conditionOrig.id] = conditionCopy.id
                conditionCopy
            },
            actions = from.actions.map { createNewActionFrom(it, eventId) }
        ).also { eventCopyConditionIdMap.clear() }
    }

    fun createNewColorCondition(context: Context, @ColorInt color: Int, detectionArea: Rect): ScreenCondition.Color {
        val id = conditionsIdCreator.generateNewIdentifier()

        return ScreenCondition.Color(
            id = id,
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            detectionArea = detectionArea,
            threshold = defaultValues.conditionThreshold(context),
            shouldBeDetected = defaultValues.conditionShouldBeDetected(),
            priority = 0,
            color = color,
        )
    }

    fun createNewNumberCondition(context: Context): ScreenCondition.Number {
        val id = conditionsIdCreator.generateNewIdentifier()

        return ScreenCondition.Number(
            id = id,
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            threshold = defaultValues.conditionThreshold(context),
            shouldBeDetected = defaultValues.conditionShouldBeDetected(),
            priority = 0,
            detectionArea = Rect(),
            comparisonOperation = ComparisonOperation.EQUALS,
            counterValue = CounterOperationValue.Number(0.0),
        )
    }

    fun createNewTextCondition(context: Context): ScreenCondition.Text {
        val id = conditionsIdCreator.generateNewIdentifier()

        return ScreenCondition.Text(
            id = id,
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            threshold = defaultValues.conditionThreshold(context),
            shouldBeDetected = defaultValues.conditionShouldBeDetected(),
            priority = 0,
            text = "",
            detectionArea = Rect(),
            alphabet = OCRAlphabet.LATIN,
        )
    }

    suspend fun createNewImageCondition(context: Context, area: Rect, bitmap: Bitmap): ScreenCondition.Image {
        val id = conditionsIdCreator.generateNewIdentifier()
        val newPath = bitmapRepository.saveImageConditionBitmap(
            bitmap = bitmap,
            prefix = CONDITION_FILE_PREFIX,
        )
        _newImageConditionsPaths.add(newPath)

        return ScreenCondition.Image(
            id = id,
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            area = area,
            threshold = defaultValues.conditionThreshold(context),
            detectionType = defaultValues.conditionDetectionType(),
            shouldBeDetected = defaultValues.conditionShouldBeDetected(),
            path = newPath,
            priority = 0,
        )
    }

    fun createNewScreenConditionFrom(condition: ScreenCondition, eventId: Identifier = getEditedEventIdOrThrow()): ScreenCondition =
        when (condition) {
            is ScreenCondition.Color -> createNewColorConditionFrom(condition, eventId)
            is ScreenCondition.Image -> createNewImageConditionFrom(condition, eventId)
            is ScreenCondition.Number -> createNewNumberConditionFrom(condition, eventId)
            is ScreenCondition.Text -> createNewTextConditionFrom(condition, eventId)
        }

    private fun createNewColorConditionFrom(condition: ScreenCondition.Color, eventId: Identifier = getEditedEventIdOrThrow()): ScreenCondition.Color =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
        )

    private fun createNewImageConditionFrom(condition: ScreenCondition.Image, eventId: Identifier = getEditedEventIdOrThrow()): ScreenCondition.Image =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            path = "" + condition.path,
        )

    private fun createNewNumberConditionFrom(condition: ScreenCondition.Number, eventId: Identifier = getEditedEventIdOrThrow()): ScreenCondition.Number =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
        )

    private fun createNewTextConditionFrom(condition: ScreenCondition.Text, eventId: Identifier = getEditedEventIdOrThrow()): ScreenCondition.Text =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            text = "" + condition.text,
        )

    fun createNewOnBroadcastReceived(context: Context): TriggerCondition.OnBroadcastReceived =
        TriggerCondition.OnBroadcastReceived(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            intentAction = "",
        )

    fun createNewOnCounterReached(context: Context): TriggerCondition.OnCounterCountReached =
        TriggerCondition.OnCounterCountReached(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            counterName = "",
            comparisonOperation = defaultValues.counterComparisonOperation(),
            counterValue = CounterOperationValue.Number(0.0)
        )

    fun createNewOnTimerReached(context: Context): TriggerCondition.OnTimerReached =
        TriggerCondition.OnTimerReached(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            durationMs = 0,
            restartWhenReached = false,
        )

    fun createNewTriggerConditionFrom(condition: TriggerCondition, eventId: Identifier = getEditedEventIdOrThrow()): TriggerCondition =
        when (condition) {
            is TriggerCondition.OnBroadcastReceived -> createNewOnBroadcastReceivedFrom(condition, eventId)
            is TriggerCondition.OnCounterCountReached -> createNewOnCounterReachedFrom(condition, eventId)
            is TriggerCondition.OnTimerReached -> createNewOnTimerReachedFrom(condition, eventId)
        }

    private fun createNewOnBroadcastReceivedFrom(condition: TriggerCondition.OnBroadcastReceived, eventId: Identifier) =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            intentAction = "" + condition.intentAction,
        )

    private fun createNewOnCounterReachedFrom(condition: TriggerCondition.OnCounterCountReached, eventId: Identifier) =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            counterName = "" + condition.counterName,
        )

    private fun createNewOnTimerReachedFrom(condition: TriggerCondition.OnTimerReached, eventId: Identifier) =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
        )

    fun createNewClick(context: Context): Click {
        val displaySize = displayConfigManager.displayConfig.sizePx

        return Click(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.clickName(context),
            pressDuration = defaultValues.clickPressDuration(context),
            position = Position.Coordinates(
                x = CoordinatesValue.Literal(displaySize.x / 2),
                y = CoordinatesValue.Literal(displaySize.y / 2),
            ),
            priority = 0,
        )
    }

    fun createNewGesture(context: Context): Gesture =
        Gesture(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.gestureName(context),
            priority = 0,
            fingers = listOf(createNewGestureFinger())
        )

    fun createNewPause(context: Context): Pause =
        Pause(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.pauseName(context),
            pauseDuration = defaultValues.pauseDuration(context),
            priority = 0,
        )

    fun createNewIntent(context: Context): Intent =
        Intent(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.intentName(context),
            isBroadcast = false,
            isAdvanced = defaultValues.intentIsAdvanced(context),
            priority = 0,
        )

    fun createNewIntentExtra() : IntentExtra<Any> =
        IntentExtra(
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = getEditedActionIdOrThrow(),
            key = null,
            value = null,
        )

    fun createNewToggleEvent(context: Context): ToggleEvent =
        ToggleEvent(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.toggleEventName(context),
            toggleAll = false,
            toggleAllType = null,
            eventToggles = emptyList(),
            priority = 0,
        )

    fun createNewEventToggle(
        id: Identifier = eventTogglesIdCreator.generateNewIdentifier(),
        targetEventId: Identifier? = null,
        toggleType: ToggleEvent.ToggleType = defaultValues.eventToggleType(),
    ) = EventToggle(
            id = id,
            actionId = getEditedActionIdOrThrow(),
            targetEventId = targetEventId,
            toggleType = toggleType,
        )

    fun createNewChangeCounter(context: Context): ChangeCounter =
        ChangeCounter(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.changeCounterName(context),
            counterName = "",
            operation = ChangeCounter.OperationType.ADD,
            operationValue = CounterOperationValue.Number(0.0),
            priority = 0,
        )

    fun createNewNotification(context: Context): Notification =
        Notification(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.notificationName(context),
            channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
            messageText = "",
            priority = 0,
        )

    fun createNewSystemAction(context: Context): SystemAction =
        SystemAction(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.systemActionName(context),
            type = SystemAction.Type.BACK,
            priority = 0,
        )

    fun createNewSetText(context: Context): SetText =
        SetText(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.setTextName(context),
            text = "",
            validateInput = false,
            priority = 0,
        )

    fun createNewActionFrom(from: Action, eventId: Identifier = getEditedEventIdOrThrow()): Action = when (from) {
        is Click -> createNewClickFrom(from, eventId)
        is Gesture -> createNewGestureFrom(from, eventId)
        is Pause -> createNewPauseFrom(from, eventId)
        is Intent -> createNewIntentFrom(from, eventId)
        is ToggleEvent -> createNewToggleEventFrom(from, eventId)
        is ChangeCounter -> createNewChangeCounterFrom(from, eventId)
        is Notification -> createNewNotificationFrom(from, eventId)
        is SystemAction -> createNewSystemActionFrom(from, eventId)
        is SetText -> createNewSetTextFrom(from, eventId)
    }

    private fun createNewClickFrom(from: Click, eventId: Identifier): Click =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
            position = from.position?.toCopiedConditionPosition(),
        )

    private fun createNewGestureFrom(from: Gesture, eventId: Identifier): Gesture =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
            fingers = from.fingers.map { finger -> createNewGestureFingerFrom(finger) }
        )

    fun newFingerIdentifier(): Identifier = gestureFingerIdCreator.generateNewIdentifier()
    fun newMotionIdentifier(): Identifier = gestureMotionIdCreator.generateNewIdentifier()

    fun createNewGestureFinger(): GestureFinger {
        val displaySize = displayConfigManager.displayConfig.sizePx
        return GestureFinger(
            id = gestureFingerIdCreator.generateNewIdentifier(),
            startTimeMs = 0,
            motions = listOf(
                Press(
                    id = gestureMotionIdCreator.generateNewIdentifier(),
                    position = Position.Coordinates(
                        x = CoordinatesValue.Literal(displaySize.x / 2),
                        y = CoordinatesValue.Literal(displaySize.y / 2),
                    ),
                    offset = Position.Coordinates(
                        x = CoordinatesValue.Literal(0),
                        y = CoordinatesValue.Literal(0),
                    ),
                ),
                Release(
                    id = gestureMotionIdCreator.generateNewIdentifier(),
                ),
            )
        )
    }

    fun createNewGestureMotionMove(): Move {
        val displaySize = displayConfigManager.displayConfig.sizePx
        return Move(
            id = gestureMotionIdCreator.generateNewIdentifier(),
            position = Position.Coordinates(
                x = CoordinatesValue.Literal(displaySize.x / 2),
                y = CoordinatesValue.Literal(displaySize.y / 2),
            ),
            offset = Position.Coordinates(
                x = CoordinatesValue.Literal(0),
                y = CoordinatesValue.Literal(0),
            ),
            durationMs = DEFAULT_GESTURE_MOTION_DURATION_MS,
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNewGestureMotionWait(): Wait =
        Wait(
            id = gestureMotionIdCreator.generateNewIdentifier(),
            durationMs = DEFAULT_GESTURE_MOTION_DURATION_MS,
        )

    private fun createNewGestureFingerFrom(from: GestureFinger): GestureFinger =
        from.copy(
            id = gestureFingerIdCreator.generateNewIdentifier(),
            motions = from.motions.mapNotNull { motion ->
                when (motion) {
                    is Move -> motion.copy(
                        id = gestureMotionIdCreator.generateNewIdentifier(),
                        position = motion.position.toCopiedConditionPosition(),
                        offset = motion.offset.toCopiedConditionPosition(),
                    )
                    is Press -> motion.copy(
                        id = gestureMotionIdCreator.generateNewIdentifier(),
                        position = motion.position.toCopiedConditionPosition(),
                        offset = motion.offset.toCopiedConditionPosition(),
                    )
                    is Release -> motion.copy(id = gestureMotionIdCreator.generateNewIdentifier())
                    is Wait ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) motion.copy(id = gestureMotionIdCreator.generateNewIdentifier())
                        else null
                }
            }
        )

    /**
     * When copying a whole event, the conditions are copied first and get new identifiers. A position targeting one
     * of them must follow to the copied condition instead of pointing back at the original event.
     */
    private fun Position.toCopiedConditionPosition(): Position {
        if (this !is Position.Condition) return this
        val mappedId = conditionId?.let { eventCopyConditionIdMap[it] } ?: return this
        return copy(conditionId = mappedId)
    }

    private fun createNewPauseFrom(from: Pause, eventId: Identifier): Pause =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewIntentFrom(from: Intent, eventId: Identifier): Intent {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            intentAction = "" + from.intentAction,
            componentName = from.componentName?.clone(),
            extras = from.extras?.map { extra -> createNewIntentExtraFrom(extra, eventId) }
        )
    }

    private fun createNewIntentExtraFrom(from: IntentExtra<out Any>, actionId: Identifier = getEditedActionIdOrThrow()): IntentExtra<out Any> =
        from.copy(
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = actionId,
            key = "" + from.key,
        )

    private fun createNewToggleEventFrom(from: ToggleEvent, eventId: Identifier): ToggleEvent {
        val actionId = actionsIdCreator.generateNewIdentifier()

        val eventsToggles = from.eventToggles.map { eventToggle ->
            createEventToggleFrom(eventToggle, actionId)
        }

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            eventToggles = eventsToggles,
        )
    }

    private fun createEventToggleFrom(from: EventToggle, actionId: Identifier = getEditedActionIdOrThrow()): EventToggle =
        from.copy(
            id = eventTogglesIdCreator.generateNewIdentifier(),
            actionId = actionId,
        )

    private fun createNewChangeCounterFrom(from: ChangeCounter, eventId: Identifier): ChangeCounter {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            counterName = "" + from.counterName,
        )
    }

    private fun createNewNotificationFrom(from: Notification, eventId: Identifier): Notification {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            messageText = "" + from.messageText,
        )
    }

    private fun createNewSystemActionFrom(from: SystemAction, eventId: Identifier): SystemAction {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            type = from.type,
        )
    }

    private fun createNewSetTextFrom(from: SetText, eventId: Identifier): SetText {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            text = from.text,
            validateInput = from.validateInput,
        )
    }

    private fun getEditedScenarioIdOrThrow(): Identifier =
        editor.editedScenario.value?.id
            ?: throw IllegalStateException("Can't create items without an edited scenario")

    private fun getEditedEventIdOrThrow(): Identifier =
        editor.currentEventEditor.value?.editedItem?.value?.id
            ?: throw IllegalStateException("Can't create items without an edited event")

    private fun getEditedActionIdOrThrow(): Identifier =
        editor.currentEventEditor.value?.actionsEditor?.editedItem?.value?.id
            ?: throw IllegalStateException("Can't create items without an edited action")

    private fun getEditedImageEventsCountOrThrow(): Int = editor.getEditedImageEventsCount()
}

private const val DEFAULT_GESTURE_MOTION_DURATION_MS = 250L