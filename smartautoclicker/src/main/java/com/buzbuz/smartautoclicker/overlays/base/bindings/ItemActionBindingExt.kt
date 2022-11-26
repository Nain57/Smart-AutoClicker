/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.base.bindings

import android.content.Context
import android.view.View

import androidx.annotation.DrawableRes

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemActionBinding
import com.buzbuz.smartautoclicker.domain.Action

import kotlin.time.Duration.Companion.milliseconds

fun ItemActionBinding.bind(
    details: ActionDetails,
    bindingAdapterPosition: Int,
    canDrag: Boolean,
    actionClickedListener: (Action, Int) -> Unit,
) {
    root.setOnClickListener { actionClickedListener.invoke(details.action, bindingAdapterPosition) }

    actionName.visibility = View.VISIBLE
    actionTypeIcon.setImageResource(details.icon)
    actionName.text = details.name
    actionDetails.text = details.details

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
)

/** @return the [ActionDetails] corresponding to this action. */
fun Action.toActionDetails(context: Context): ActionDetails {
    val item = when (this) {
        is Action.Click -> ActionDetails(
            icon = R.drawable.ic_click,
            name = name!!,
            details = if (clickOnCondition) context.getString(
                R.string.dialog_action_config_click_position_on_condition
            ) else context.getString(
                R.string.dialog_action_copy_click_details,
                formatDuration(pressDuration!!), x, y
            ),
            action = this,
        )

        is Action.Swipe -> ActionDetails(
            icon = R.drawable.ic_swipe,
            name = name!!,
            details = context.getString(
                R.string.dialog_action_copy_swipe_details,
                formatDuration(swipeDuration!!), fromX, fromY, toX, toY
            ),
            action = this,
        )

        is Action.Pause -> ActionDetails(
            icon = R.drawable.ic_wait,
            name = name!!,
            details = context.getString(
                R.string.dialog_action_copy_pause_details,
                formatDuration(pauseDuration!!)
            ),
            action = this,
        )

        is Action.Intent -> ActionDetails(
            icon = R.drawable.ic_intent,
            name = name!!,
            details = formatIntentDetails(this, context),
            action = this,
        )
    }

    return item
}

/**
 * Format a duration into a human readable string.
 * @param msDuration the duration to be formatted in milliseconds.
 * @return the formatted duration.
 */
private fun formatDuration(msDuration: Long): String {
    val duration = msDuration.milliseconds
    var value = ""
    if (duration.inWholeHours > 0) {
        value += "${duration.inWholeHours}h "
    }
    if (duration.inWholeMinutes % 60 > 0) {
        value += "${duration.inWholeMinutes % 60}m"
    }
    if (duration.inWholeSeconds % 60 > 0) {
        value += "${duration.inWholeSeconds % 60}s"
    }
    if (duration.inWholeMilliseconds % 1000 > 0) {
        value += "${duration.inWholeMilliseconds % 1000}ms"
    }

    return value.trim()
}

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
                        R.string.dialog_action_copy_intent_details_action_component,
                        action,
                        componentName,
                    )
                }
            }
        }
    }

    return context.getString(R.string.dialog_action_copy_intent_details_action, action)
}

/** */
private const val INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT = 15
/** */
private const val INTENT_COMPONENT_DISPLAYED_COMPONENT_LENGTH_LIMIT = 20