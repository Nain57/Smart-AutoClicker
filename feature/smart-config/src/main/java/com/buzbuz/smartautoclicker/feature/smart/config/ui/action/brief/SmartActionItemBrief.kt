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

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R


fun Action.toActionBrief(context: Context, inError: Boolean = !isComplete()): ItemBrief = when (this) {
    is Action.Click -> toClickBrief(context, inError)
    is Action.Swipe -> toSwipeBrief(context, inError)
    is Action.Pause -> toPauseBrief(context, inError)
    is Action.Intent -> toIntentBrief(context, inError)
    is Action.ToggleEvent -> toToggleEventBrief(context, inError)
    is Action.ChangeCounter -> toChangeCounterBrief(context, inError)
}

private fun Action.Click.toClickBrief(context: Context, inError: Boolean): ItemBrief =
    ItemBrief(
        id = id,
        data = this,
        icon = R.drawable.ic_click,
        name = name ?: "Click",
        description = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            positionType == Action.Click.PositionType.ON_DETECTED_CONDITION ->
                context.getString(R.string.item_click_details_on_condition)
            else  -> context.getString(
                R.string.item_click_details_at_position,
                formatDuration(pressDuration!!), x, y,
            )
        },
        inError = inError,
    )

private fun Action.Swipe.toSwipeBrief(context: Context, inError: Boolean): ItemBrief =
    ItemBrief(
        id = id,
        data = this,
        icon = R.drawable.ic_swipe,
        name = name ?: "Swipe",
        description = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            else -> context.getString(
                R.string.item_swipe_details,
                formatDuration(swipeDuration!!), fromX, fromY, toX, toY
            )
        },
        inError = inError,
    )

private fun Action.Pause.toPauseBrief(context: Context, inError: Boolean): ItemBrief =
    ItemBrief(
        id = id,
        data = this,
        icon = R.drawable.ic_wait,
        name = name ?: "Wait",
        description = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            else -> context.getString(
                R.string.item_pause_details,
                formatDuration(pauseDuration!!)
            )
        },
        inError = inError,
    )

private fun Action.Intent.toIntentBrief(context: Context, inError: Boolean): ItemBrief =
    ItemBrief(
        id = id,
        data = this,
        icon = R.drawable.ic_intent,
        name = name ?: "Intent",
        description = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            else -> formatIntentDetails(this, context)
        },
        inError = inError,
    )


private fun Action.ToggleEvent.toToggleEventBrief(context: Context, inError: Boolean): ItemBrief =
    ItemBrief(
        id = id,
        data = this,
        icon = R.drawable.ic_toggle_event,
        name = name!!,
        description = when {
            inError -> context.getString(R.string.item_toggle_event_details_error)
            else -> formatToggleEventState(this, context)
        },
        inError = inError,
    )

private fun Action.ChangeCounter.toChangeCounterBrief(context: Context, inError: Boolean): ItemBrief =
    ItemBrief(
        id = id,
        data = this,
        icon = R.drawable.ic_change_counter,
        name = name!!,
        description = when {
            inError -> context.getString(R.string.item_change_counter_details_error)
            else -> formatChangeCounter(this, context)
        },
        inError = inError,
    )


private fun formatIntentDetails(intent: Action.Intent, context: Context): String {
    var action = intent.intentAction ?: return ""

    val dotIndex = action.lastIndexOf('.')
    if (dotIndex != -1 && dotIndex < action.lastIndex) {
        action = action.substring(dotIndex + 1)

        if (!intent.isBroadcast && intent.componentName != null && action.length < INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT) {
            var componentName = intent.componentName!!.flattenToString()
            val dotIndex2 = componentName.lastIndexOf('.')
            if (dotIndex2 != -1 && dotIndex2 < componentName.lastIndex) {

                componentName = componentName.substring(dotIndex2 + 1)
                if (componentName.length < INTENT_COMPONENT_DISPLAYED_COMPONENT_LENGTH_LIMIT) {
                    return context.getString(
                        R.string.item_intent_details_component_name,
                        action,
                        componentName,
                    )
                }
            }
        }
    }

    return context.getString(R.string.item_intent_details, action)
}

private fun formatToggleEventState(toggleEvent: Action.ToggleEvent, context: Context): String =
    if (toggleEvent.toggleAll) {
        when (toggleEvent.toggleAllType) {
            Action.ToggleEvent.ToggleType.ENABLE -> context.getString(R.string.item_toggle_event_details_enable_all)
            Action.ToggleEvent.ToggleType.TOGGLE -> context.getString(R.string.item_toggle_event_details_invert_all)
            Action.ToggleEvent.ToggleType.DISABLE -> context.getString(R.string.item_toggle_event_details_disable_all)
            null -> throw IllegalArgumentException("Invalid toggle event type")
        }
    } else {
        context.getString(
            R.string.item_toggle_event_details_manual,
            toggleEvent.eventToggles.size,
        )
    }

private fun formatChangeCounter(changeCounter: Action.ChangeCounter, context: Context): String =
    context.getString(
        R.string.item_change_counter_details,
        changeCounter.counterName.trim(),
        when (changeCounter.operation) {
            Action.ChangeCounter.OperationType.ADD -> "+"
            Action.ChangeCounter.OperationType.MINUS -> "-"
            Action.ChangeCounter.OperationType.SET -> "="
        },
        changeCounter.operationValue,
    )

/** The maximal length of the displayed intent action string. */
private const val INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT = 15
/** The maximal length of the displayed intent component name string. */
private const val INTENT_COMPONENT_DISPLAYED_COMPONENT_LENGTH_LIMIT = 20