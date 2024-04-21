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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import android.content.Context
import android.util.TypedValue
import android.view.View

import androidx.annotation.DrawableRes

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemActionBinding

fun ItemActionBinding.bind(
    details: ActionDetails,
    canDrag: Boolean,
    actionClickedListener: (Action) -> Unit,
) {
    root.setOnClickListener { actionClickedListener.invoke(details.action) }

    actionName.visibility = View.VISIBLE
    actionTypeIcon.setImageResource(details.icon)
    actionName.text = details.name
    actionDetails.apply {
        text = details.details

        val typedValue = TypedValue()
        val actionColorAttr = if (details.haveError) R.attr.colorError else R.attr.colorOnSurfaceVariant
        root.context.theme.resolveAttribute(actionColorAttr, typedValue, true)
        setTextColor(typedValue.data)
    }

    btnReorder.visibility = if (canDrag) View.VISIBLE else View.GONE
}

/**
 * Action item.
 * @param icon the icon for the action.
 * @param name the name of the action.
 * @param details the details for the action.
 * @param action the action represented by this item.
 */
data class ActionDetails (
    @DrawableRes val icon: Int,
    val name: String,
    val details: String,
    val action: Action,
    val haveError: Boolean,
)

/** @return the [ActionDetails] corresponding to this action. */
fun Action.toActionDetails(context: Context, inError: Boolean = !isComplete()): ActionDetails = when (this) {
    is Action.Click -> toClickDetails(context, inError)
    is Action.Swipe -> toSwipeDetails(context, inError)
    is Action.Pause -> toPauseDetails(context, inError)
    is Action.Intent -> toIntentDetails(context, inError)
    is Action.ToggleEvent -> toToggleEventDetails(context, inError)
    is Action.ChangeCounter -> toChangeCounterDetails(context, inError)
    else -> throw IllegalArgumentException("Not yet supported")
}

private fun Action.Click.toClickDetails(context: Context, inError: Boolean): ActionDetails =
    ActionDetails(
        icon = R.drawable.ic_click,
        name = name!!,
        details = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            positionType == Action.Click.PositionType.ON_DETECTED_CONDITION ->
                context.getString(R.string.item_desc_click_position_on_condition)
            else  -> context.getString(
                R.string.item_desc_click_details,
                formatDuration(pressDuration!!), x, y,
            )
        },
        action = this,
        haveError = inError,
    )

private fun Action.Swipe.toSwipeDetails(context: Context, inError: Boolean): ActionDetails =
    ActionDetails(
        icon = R.drawable.ic_swipe,
        name = name!!,
        details = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            else -> context.getString(
                R.string.item_desc_swipe_details,
                formatDuration(swipeDuration!!), fromX, fromY, toX, toY
            )
        },
        action = this,
        haveError = inError,
    )

private fun Action.Pause.toPauseDetails(context: Context, inError: Boolean): ActionDetails =
    ActionDetails(
        icon = R.drawable.ic_wait,
        name = name!!,
        details = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            else -> context.getString(
                R.string.item_desc_pause_details,
                formatDuration(pauseDuration!!)
            )
        },
        action = this,
        haveError = inError,
    )

private fun Action.Intent.toIntentDetails(context: Context, inError: Boolean): ActionDetails =
    ActionDetails(
        icon = R.drawable.ic_intent,
        name = name!!,
        details = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            else -> formatIntentDetails(this, context)
        },
        action = this,
        haveError = inError,
    )


private fun Action.ToggleEvent.toToggleEventDetails(context: Context, inError: Boolean): ActionDetails =
    ActionDetails(
        icon = R.drawable.ic_toggle_event,
        name = name!!,
        details = when {
            inError -> context.getString(R.string.item_error_action_invalid_toggle_event_target)
            else -> formatToggleEventState(this, context)
        },
        action = this,
        haveError = inError,
    )

private fun Action.ChangeCounter.toChangeCounterDetails(context: Context, inError: Boolean): ActionDetails =
    ActionDetails(
        icon = R.drawable.ic_change_counter,
        name = name!!,
        details = when {
            inError -> context.getString(R.string.item_error_action_invalid_change_counter)
            else -> formatChangeCounter(this, context)
        },
        action = this,
        haveError = inError,
    )

/**
 * Format a action intent into a human readable string.
 * @param intent the action intent to be formatted.
 * @return the formatted intent.
 */
private fun formatIntentDetails(intent: Action.Intent, context: Context): String {
    var action = intent.intentAction ?: return ""

    val dotIndex = action.lastIndexOf('.')
    if (dotIndex != -1 && dotIndex < action.lastIndex) {
        action = action.substring(dotIndex + 1)

        if (intent.isBroadcast == false && intent.componentName != null
            && action.length < INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT
        ) {

            var componentName = intent.componentName!!.flattenToString()
            val dotIndex2 = componentName.lastIndexOf('.')
            if (dotIndex2 != -1 && dotIndex2 < componentName.lastIndex) {

                componentName = componentName.substring(dotIndex2 + 1)
                if (componentName.length < INTENT_COMPONENT_DISPLAYED_COMPONENT_LENGTH_LIMIT) {
                    return context.getString(
                        R.string.item_desc_intent_action_component,
                        action,
                        componentName,
                    )
                }
            }
        }
    }

    return context.getString(R.string.item_desc_intent_action, action)
}

/**
 * Format a action toggle event into a human readable string.
 * @param toggleEvent the action intent to be formatted.
 * @param context the Android context.
 * @return the formatted toggle event action.
 */
private fun formatToggleEventState(toggleEvent: Action.ToggleEvent, context: Context): String =
    if (toggleEvent.toggleAll) {
        when (toggleEvent.toggleAllType) {
            Action.ToggleEvent.ToggleType.ENABLE -> context.getString(R.string.item_desc_toggle_event_state_enable_all)
            Action.ToggleEvent.ToggleType.TOGGLE -> context.getString(R.string.item_desc_toggle_event_state_invert_all)
            Action.ToggleEvent.ToggleType.DISABLE -> context.getString(R.string.item_desc_toggle_event_state_disable_all)
            null -> throw IllegalArgumentException("Invalid toggle event type")
        }
    } else {
        context.getString(
            R.string.item_desc_toggle_event_state_manual,
            toggleEvent.eventToggles.size,
        )
    }

private fun formatChangeCounter(changeCounter: Action.ChangeCounter, context: Context): String =
    context.getString(
        R.string.item_desc_change_counter_details,
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