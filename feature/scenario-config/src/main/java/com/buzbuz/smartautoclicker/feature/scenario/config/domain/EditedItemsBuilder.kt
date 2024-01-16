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
package com.buzbuz.smartautoclicker.feature.scenario.config.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor
import com.buzbuz.smartautoclicker.core.base.identifier.IdentifierCreator
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Click.PositionType
import com.buzbuz.smartautoclicker.core.domain.model.action.EventToggle

class EditedItemsBuilder internal constructor(
    context: Context,
    private val editor: ScenarioEditor,
) {

    private val defaultValues = EditionDefaultValues(context)
    private val eventsIdCreator = IdentifierCreator()
    private val conditionsIdCreator = IdentifierCreator()
    private val actionsIdCreator = IdentifierCreator()
    private val intentExtrasIdCreator = IdentifierCreator()
    private val endConditionsIdCreator = IdentifierCreator()

    /**
     * Map of original condition list ids to copy condition ids.
     * Will contain data only when creating an event from another one.
     */
    private val eventCopyConditionIdMap =  mutableMapOf<Identifier, Identifier>()

    internal fun resetGeneratedIdsCount() {
        eventsIdCreator.resetIdCount()
        conditionsIdCreator.resetIdCount()
        actionsIdCreator.resetIdCount()
        intentExtrasIdCreator.resetIdCount()
        endConditionsIdCreator.resetIdCount()
    }

    fun createNewEvent(context: Context): ImageEvent =
        ImageEvent(
            id = eventsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = defaultValues.eventName(context),
            conditionOperator = defaultValues.eventConditionOperator(),
            priority = getEditedEventsCountOrThrow(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
        )

    fun createNewEventFrom(from: ImageEvent, scenarioId: Identifier = getEditedScenarioIdOrThrow()): ImageEvent {
        val eventId = eventsIdCreator.generateNewIdentifier()

        return from.copy(
            id = eventId,
            scenarioId = scenarioId,
            name = "" + from.name,
            conditions = from.conditions.map { conditionOrig ->
                val conditionCopy = createNewConditionFrom(conditionOrig, eventId)
                eventCopyConditionIdMap[conditionOrig.id] = conditionCopy.id
                conditionCopy
            },
            actions = from.actions.map { createNewActionFrom(it, eventId) }
        ).also { eventCopyConditionIdMap.clear() }
    }

    fun createNewCondition(context: Context, area: Rect, bitmap: Bitmap): ImageCondition =
        ImageCondition(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            bitmap = bitmap,
            area = area,
            threshold = defaultValues.conditionThreshold(context),
            detectionType = defaultValues.conditionDetectionType(),
            shouldBeDetected = defaultValues.conditionShouldBeDetected(),
        )

    fun createNewConditionFrom(condition: ImageCondition, eventId: Identifier = getEditedEventIdOrThrow()) =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            path = if (condition.path != null) "" + condition.path else null,
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

    fun createEventToggle() : EventToggle =
        EventToggle(
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = getEditedActionIdOrThrow(),
            targetEventId = null,
            toggleType = defaultValues.eventToggleType(),
        )

    fun createNewActionFrom(from: Action, eventId: Identifier = getEditedEventIdOrThrow()): Action = when (from) {
        is Action.Click -> createNewClickFrom(from, eventId)
        is Action.Swipe -> createNewSwipeFrom(from, eventId)
        is Action.Pause -> createNewPauseFrom(from, eventId)
        is Action.Intent -> createNewIntentFrom(from, eventId)
        is Action.ToggleEvent -> createNewToggleEventFrom(from, eventId)
        is Action.ChangeCounter -> TODO()
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
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = actionId,
        )

    private fun isEventIdValidInEditedScenario(eventId: Identifier): Boolean =
        editor.eventsEditor.editedList.value?.let { events ->
            events.find { eventId == it.id } != null
        } ?: false

    private fun getEditedScenarioIdOrThrow(): Identifier = editor.editedScenario.value?.id
        ?: throw IllegalStateException("Can't create items without an edited scenario")

    private fun getEditedEventsCountOrThrow(): Int = editor.eventsEditor.editedList.value?.size
        ?: throw IllegalStateException("Can't create items without an edited event list")

    private fun getEditedEventIdOrThrow(): Identifier = editor.eventsEditor.editedItem.value?.id
        ?: throw IllegalStateException("Can't create items without an edited event")

    private fun getEditedActionIdOrThrow(): Identifier = editor.eventsEditor.actionsEditor.editedItem.value?.id
        ?: throw IllegalStateException("Can't create items without an edited action")
}