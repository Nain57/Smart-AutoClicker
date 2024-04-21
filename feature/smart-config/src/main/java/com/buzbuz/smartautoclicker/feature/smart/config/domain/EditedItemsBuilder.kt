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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.identifier.IdentifierCreator
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Click.PositionType
import com.buzbuz.smartautoclicker.core.domain.model.action.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.data.ScenarioEditor

class EditedItemsBuilder internal constructor(
    private val repository: IRepository,
    private val editor: com.buzbuz.smartautoclicker.feature.smart.config.data.ScenarioEditor,
) {

    private val defaultValues = EditionDefaultValues(repository)
    private val eventsIdCreator = IdentifierCreator()
    private val conditionsIdCreator = IdentifierCreator()
    private val actionsIdCreator = IdentifierCreator()
    private val intentExtrasIdCreator = IdentifierCreator()
    private val eventTogglesIdCreator = IdentifierCreator()
    private val endConditionsIdCreator = IdentifierCreator()

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
        eventCopyConditionIdMap.clear()
        _newImageConditionsPaths.clear()
    }

    fun createNewImageEvent(context: Context): ImageEvent =
        ImageEvent(
            id = eventsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = defaultValues.eventName(context),
            conditionOperator = defaultValues.eventConditionOperator(),
            priority = getEditedImageEventsCountOrThrow(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
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

    fun createNewImageEventFrom(from: ImageEvent, scenarioId: Identifier = getEditedScenarioIdOrThrow()): ImageEvent {
        val eventId = eventsIdCreator.generateNewIdentifier()

        return from.copy(
            id = eventId,
            scenarioId = scenarioId,
            name = "" + from.name,
            conditions = from.conditions.map { conditionOrig ->
                val conditionCopy = createNewImageConditionFrom(conditionOrig, eventId)
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

    suspend fun createNewImageCondition(context: Context, area: Rect, bitmap: Bitmap): ImageCondition {
        val id = conditionsIdCreator.generateNewIdentifier()
        val newPath = repository.saveConditionBitmap(bitmap)
        _newImageConditionsPaths.add(newPath)

        return ImageCondition(
            id = id,
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            area = area,
            threshold = defaultValues.conditionThreshold(context),
            detectionType = defaultValues.conditionDetectionType(),
            shouldBeDetected = defaultValues.conditionShouldBeDetected(),
            path = newPath,
        )
    }

    fun createNewImageConditionFrom(condition: ImageCondition, eventId: Identifier = getEditedEventIdOrThrow()): ImageCondition =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            path = "" + condition.path,
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
            counterValue = 0,
            comparisonOperation = defaultValues.counterComparisonOperation()
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

    fun createNewClick(context: Context): Action.Click =
        Action.Click(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.clickName(context),
            pressDuration = defaultValues.clickPressDuration(context),
            positionType = defaultValues.clickPositionType(),
            priority = 0,
        )

    fun createNewSwipe(context: Context): Action.Swipe =
        Action.Swipe(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.swipeName(context),
            swipeDuration = defaultValues.swipeDuration(context),
            priority = 0,
        )

    fun createNewPause(context: Context): Action.Pause =
        Action.Pause(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.pauseName(context),
            pauseDuration = defaultValues.pauseDuration(context),
            priority = 0,
        )

    fun createNewIntent(context: Context): Action.Intent =
        Action.Intent(
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

    fun createNewToggleEvent(context: Context): Action.ToggleEvent =
        Action.ToggleEvent(
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
        toggleType: Action.ToggleEvent.ToggleType = defaultValues.eventToggleType(),
    ) = EventToggle(
            id = id,
            actionId = getEditedActionIdOrThrow(),
            targetEventId = targetEventId,
            toggleType = toggleType,
        )

    fun createNewChangeCounter(context: Context): Action.ChangeCounter =
        Action.ChangeCounter(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.changeCounterName(context),
            counterName = "",
            operation = Action.ChangeCounter.OperationType.ADD,
            operationValue = 0,
            priority = 0,
        )

    fun createNewActionFrom(from: Action, eventId: Identifier = getEditedEventIdOrThrow()): Action = when (from) {
        is Action.Click -> createNewClickFrom(from, eventId)
        is Action.Swipe -> createNewSwipeFrom(from, eventId)
        is Action.Pause -> createNewPauseFrom(from, eventId)
        is Action.Intent -> createNewIntentFrom(from, eventId)
        is Action.ToggleEvent -> createNewToggleEventFrom(from, eventId)
        is Action.ChangeCounter -> createNewChangeCounterFrom(from, eventId)
    }

    private fun createNewClickFrom(from: Action.Click, eventId: Identifier): Action.Click {
        val conditionId =
            if (from.positionType == PositionType.ON_DETECTED_CONDITION && from.clickOnConditionId != null)
                eventCopyConditionIdMap[from.clickOnConditionId]
            else null

        return from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
            clickOnConditionId = conditionId,
        )
    }

    private fun createNewSwipeFrom(from: Action.Swipe, eventId: Identifier): Action.Swipe =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewPauseFrom(from: Action.Pause, eventId: Identifier): Action.Pause =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewIntentFrom(from: Action.Intent, eventId: Identifier): Action.Intent {
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

    private fun createNewToggleEventFrom(from: Action.ToggleEvent, eventId: Identifier): Action.ToggleEvent {
        val actionId = actionsIdCreator.generateNewIdentifier()

        val eventsToggles = from.eventToggles.mapNotNull { eventToggle ->
            // Check if the current edited scenario contains the event modified by the child event toggle.
            // Filter if not
            if (eventToggle.targetEventId == eventId || isEventIdValidInEditedScenario(eventId)) {
                createEventToggleFrom(eventToggle, actionId)
            } else null
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

    private fun createNewChangeCounterFrom(from: Action.ChangeCounter, eventId: Identifier): Action.ChangeCounter {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            counterName = "" + from.counterName,
        )
    }

    private fun isEventIdValidInEditedScenario(eventId: Identifier): Boolean =
        editor.getAllEditedEvents().find { eventId == it.id } != null

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