/*
 * Copyright (C) 2023 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.IdentifierCreator
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getIntentIsAdvancedConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getPauseDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getSwipeDurationConfig

class EditedItemsBuilder internal constructor(private val editor: ScenarioEditor) {

    private val eventsIdCreator = IdentifierCreator()
    private val conditionsIdCreator = IdentifierCreator()
    private val actionsIdCreator = IdentifierCreator()
    private val intentExtrasIdCreator = IdentifierCreator()
    private val endConditionsIdCreator = IdentifierCreator()

    internal fun resetGeneratedIdsCount() {
        eventsIdCreator.resetIdCount()
        conditionsIdCreator.resetIdCount()
        actionsIdCreator.resetIdCount()
        intentExtrasIdCreator.resetIdCount()
        endConditionsIdCreator.resetIdCount()
    }

    fun createNewEvent(context: Context): Event =
        Event(
            id = eventsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getString(R.string.default_event_name),
            conditionOperator = AND,
            priority = getEditedEventsCountOrThrow(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
        )

    fun createNewEventFrom(from: Event, scenarioId: Identifier = getEditedScenarioIdOrThrow()): Event {
        val eventId = eventsIdCreator.generateNewIdentifier()

        return from.copy(
            id = eventId,
            scenarioId = scenarioId,
            name = "" + from.name,
            conditions = from.conditions.map { createNewConditionFrom(it, eventId) },
            actions = from.actions.map { createNewActionFrom(it, eventId) }
        )
    }

    fun createNewCondition(context: Context, area: Rect, bitmap: Bitmap): Condition =
        Condition(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = context.resources.getString(R.string.default_condition_name),
            bitmap = bitmap,
            area = area,
            threshold = context.resources.getInteger(R.integer.default_condition_threshold),
            detectionType = EXACT,
            shouldBeDetected = true,
        )

    fun createNewConditionFrom(condition: Condition, eventId: Identifier = getEditedEventIdOrThrow()) =
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
            name = context.getString(R.string.default_click_name),
            pressDuration = context.getEventConfigPreferences().getClickPressDurationConfig(context),
            clickOnCondition = false,
        )

    fun createNewSwipe(context: Context): Action.Swipe =
        Action.Swipe(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = context.getString(R.string.default_swipe_name),
            swipeDuration = context.getEventConfigPreferences().getSwipeDurationConfig(context),
        )

    fun createNewPause(context: Context): Action.Pause =
        Action.Pause(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = context.getString(R.string.default_pause_name),
            pauseDuration = context.getEventConfigPreferences().getPauseDurationConfig(context)
        )

    fun createNewIntent(context: Context): Action.Intent =
        Action.Intent(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = context.getString(R.string.default_intent_name),
            isAdvanced = context.getEventConfigPreferences().getIntentIsAdvancedConfig(context),
        )

    fun createNewToggleEvent(context: Context): Action.ToggleEvent =
        Action.ToggleEvent(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = context.getString(R.string.default_toggle_event_name),
            toggleEventType = Action.ToggleEvent.ToggleType.ENABLE,
        )

    fun createNewActionFrom(from: Action, eventId: Identifier = getEditedEventIdOrThrow()): Action = when (from) {
        is Action.Click -> createNewClickFrom(from, eventId)
        is Action.Swipe -> createNewSwipeFrom(from, eventId)
        is Action.Pause -> createNewPauseFrom(from, eventId)
        is Action.Intent -> createNewIntentFrom(from, eventId)
        is Action.ToggleEvent -> createNewToggleEventFrom(from, eventId)
    }

    private fun createNewClickFrom(from: Action.Click, eventId: Identifier): Action.Click =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

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

    private fun createNewToggleEventFrom(from: Action.ToggleEvent, eventId: Identifier): Action.ToggleEvent =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    fun createNewIntentExtra() : IntentExtra<Any> =
        IntentExtra(
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = getEditedActionIdOrThrow(),
            key = null,
            value = null,
        )

    fun createNewIntentExtraFrom(from: IntentExtra<out Any>, actionId: Identifier = getEditedActionIdOrThrow()): IntentExtra<out Any> =
        from.copy(
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = actionId,
            key = "" + from.key,
        )

    fun createNewEndCondition(): EndCondition =
        EndCondition(
            id = endConditionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
        )

    fun createNewEndConditionFrom(from: EndCondition, scenarioId: Identifier = getEditedScenarioIdOrThrow()): EndCondition =
        from.copy(
            id = endConditionsIdCreator.generateNewIdentifier(),
            scenarioId = scenarioId,
            eventId = from.eventId?.copy(),
            eventName = from.eventName?.let { "" + it },
        )

    private fun getEditedScenarioIdOrThrow(): Identifier = editor.editedScenario.value?.id
        ?: throw IllegalStateException("Can't create items without an edited scenario")

    private fun getEditedEventsCountOrThrow(): Int = editor.eventsEditor.editedList.value?.size
        ?: throw IllegalStateException("Can't create items without an edited event list")

    private fun getEditedEventIdOrThrow(): Identifier = editor.eventsEditor.editedItem.value?.id
        ?: throw IllegalStateException("Can't create items without an edited event")

    private fun getEditedActionIdOrThrow(): Identifier = editor.eventsEditor.actionsEditor.editedItem.value?.id
        ?: throw IllegalStateException("Can't create items without an edited action")
}