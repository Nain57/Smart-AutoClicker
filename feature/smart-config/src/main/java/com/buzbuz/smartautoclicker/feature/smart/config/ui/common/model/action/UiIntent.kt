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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.smart.config.R
import kotlin.reflect.KClass


@Suppress("UnusedReceiverParameter")
internal val Action.Intent.iconRes: Int
    @DrawableRes get() = R.drawable.ic_intent

internal fun Action.Intent.getDescription(context: Context, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)

    var action = intentAction ?: return ""

    val dotIndex = action.lastIndexOf('.')
    if (dotIndex != -1 && dotIndex < action.lastIndex) {
        action = action.substring(dotIndex + 1)

        if (!isBroadcast && componentName != null
            && action.length < INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT
        ) {

            var componentName = componentName!!.flattenToString()
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

@StringRes
internal fun KClass<out Any>.getIntentExtraTypeDisplayName(): Int = when (this) {
    Byte::class -> R.string.dropdown_intent_extra_type_item_byte
    Boolean::class -> R.string.dropdown_intent_extra_type_item_boolean
    Char::class -> R.string.dropdown_intent_extra_type_item_char
    Double::class -> R.string.dropdown_intent_extra_type_item_double
    Int::class -> R.string.dropdown_intent_extra_type_item_int
    Float::class -> R.string.dropdown_intent_extra_type_item_float
    Short::class -> R.string.dropdown_intent_extra_type_item_short
    String::class -> R.string.dropdown_intent_extra_type_item_string
    else -> 0
}

/** The maximal length of the displayed intent action string. */
private const val INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT = 15
/** The maximal length of the displayed intent component name string. */
private const val INTENT_COMPONENT_DISPLAYED_COMPONENT_LENGTH_LIMIT = 20