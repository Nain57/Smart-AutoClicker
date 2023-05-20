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
package com.buzbuz.smartautoclicker.feature.scenario.config.data

import android.content.Context

import com.buzbuz.smartautoclicker.domain.model.Identifier
import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.IdentifierCreator
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getIntentIsAdvancedConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getPauseDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getSwipeDurationConfig

internal class ActionsEditor : ListEditor<ActionsEditor.Reference, Action>() {

    /** Manages the identifiers for the newly created intent extras. */
    private val intentExtraIdCreator = IdentifierCreator()

    override fun createReferenceFromEdition(): Reference =
        Reference(
            eventId = getReferenceOrThrow().eventId,
            actions = editedValue.value ?: emptyList(),
        )

    override fun getValueFromReference(reference: Reference): List<Action> =
        reference.actions

    override fun itemMatcher(first: Action, second: Action): Boolean =
        first.id == second.id

    fun createNewClick(context: Context, from: Action.Click?): Action.Click =
        from?.let { createNewClickFrom(it, getReferenceOrThrow().eventId) }
            ?: Action.Click(
                id = generateNewIdentifier(),
                eventId = getReferenceOrThrow().eventId,
                name = context.getString(R.string.default_click_name),
                pressDuration = context.getEventConfigPreferences().getClickPressDurationConfig(context),
                clickOnCondition = false,
            )

    fun createNewSwipe(context: Context, from: Action.Swipe?): Action.Swipe =
        from?.let { createNewSwipeFrom(it, getReferenceOrThrow().eventId) }
            ?: Action.Swipe(
                id = generateNewIdentifier(),
                eventId = getReferenceOrThrow().eventId,
                name = context.getString(R.string.default_swipe_name),
                swipeDuration = context.getEventConfigPreferences().getSwipeDurationConfig(context),
            )

    fun createNewPause(context: Context, from: Action.Pause?): Action.Pause =
        from?.let { createNewPauseFrom(it, getReferenceOrThrow().eventId) }
            ?: Action.Pause(
                id = generateNewIdentifier(),
                eventId = getReferenceOrThrow().eventId,
                name = context.getString(R.string.default_pause_name),
                pauseDuration = context.getEventConfigPreferences().getPauseDurationConfig(context)
            )

    fun createNewIntent(context: Context, from: Action.Intent?): Action.Intent =
        from?.let { createNewIntentFrom(it, getReferenceOrThrow().eventId) }
            ?: Action.Intent(
                id = generateNewIdentifier(),
                eventId = getReferenceOrThrow().eventId,
                name = context.getString(R.string.default_intent_name),
                isAdvanced = context.getEventConfigPreferences().getIntentIsAdvancedConfig(context),
            )

    fun createNewToggleEvent(context: Context, from: Action.ToggleEvent?): Action.ToggleEvent =
        from?.let { createNewToggleEventFrom(it, getReferenceOrThrow().eventId) }
            ?: Action.ToggleEvent(
                id = generateNewIdentifier(),
                eventId = getReferenceOrThrow().eventId,
                name = context.getString(R.string.default_toggle_event_name),
                toggleEventType = Action.ToggleEvent.ToggleType.ENABLE,
            )

    fun createNewItemsFrom(items: List<Action>, eventId: Identifier) =
        items.map { condition -> createNewItemFrom(condition, eventId) }

    fun createNewItemFrom(from: Action, eventId: Identifier): Action = when (from) {
        is Action.Click -> createNewClickFrom(from, eventId)
        is Action.Swipe -> createNewSwipeFrom(from, eventId)
        is Action.Pause -> createNewPauseFrom(from, eventId)
        is Action.Intent -> createNewIntentFrom(from, eventId)
        is Action.ToggleEvent -> createNewToggleEventFrom(from, eventId)
    }

    private fun createNewClickFrom(from: Action.Click, eventId: Identifier): Action.Click =
        from.copy(
            id = generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewSwipeFrom(from: Action.Swipe, eventId: Identifier): Action.Swipe =
        from.copy(
            id = generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewPauseFrom(from: Action.Pause, eventId: Identifier): Action.Pause =
        from.copy(
            id = generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewIntentFrom(from: Action.Intent, eventId: Identifier): Action.Intent {
        val actionId = generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            intentAction = "" + from.intentAction,
            componentName = from.componentName?.clone(),
            extras = createNewIntentExtrasFrom(from).toMutableList()
        )
    }

    private fun createNewToggleEventFrom(from: Action.ToggleEvent, eventId: Identifier): Action.ToggleEvent =
        from.copy(
            id = generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    fun createNewIntentExtra(actionId: Identifier) : IntentExtra<Any> =
        IntentExtra(
            id = intentExtraIdCreator.generateNewIdentifier(),
            actionId = actionId,
            key = null,
            value = null,
        )

    private fun createNewIntentExtrasFrom(from: Action.Intent): List<IntentExtra<out Any>> =
        from.extras?.map { extra -> createNewIntentExtraFrom(extra, from.id) } ?: emptyList()

    private fun createNewIntentExtraFrom(from: IntentExtra<out Any>, actionId: Identifier): IntentExtra<out Any> =
        from.copy(
            id = intentExtraIdCreator.generateNewIdentifier(),
            actionId = actionId,
            key = "" + from.key,
        )

    internal data class Reference(
        val eventId: Identifier,
        val actions: List<Action>,
    )
}