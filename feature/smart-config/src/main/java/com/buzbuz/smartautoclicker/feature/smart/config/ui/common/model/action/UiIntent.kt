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
import android.content.Intent as AndroidIntent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.core.android.application.getApplicationLabel
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.feature.smart.config.R

import kotlin.reflect.KClass


@DrawableRes
internal fun getIntentIconRes(): Int =
    R.drawable.ic_intent

internal fun Intent.getDescription(context: Context, inError: Boolean): String {
    if (inError) {
        return context.getString(R.string.item_error_action_invalid_generic)
    }

    val compName = componentName
    if (!isBroadcast && compName != null) {
        val appName = context.packageManager.getApplicationLabel(AndroidIntent(intentAction).setComponent(compName))
        if (appName != null) {
            return context.getString(R.string.item_intent_details_start_activity, appName)
        }
    }

    val displayAction = intentAction?.getLastPart('.')
    val displayComponentName = componentName?.flattenToString()?.getLastPart('.')
    if (displayAction != null && displayComponentName != null) {
        return context.getString(R.string.item_intent_details_component_name, displayAction, displayComponentName)
    }

    return context.getString(R.string.item_intent_details, displayAction ?: intentAction)
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

private fun String.getLastPart(separator: Char): String? {
    val separatorIndex = lastIndexOf(separator)

    return if (separatorIndex != -1 && separatorIndex < lastIndex) {
        substring(separatorIndex + 1)
    } else null
}
