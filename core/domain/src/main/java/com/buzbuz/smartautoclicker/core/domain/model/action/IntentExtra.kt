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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.content.Intent

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * Extras for a Intent action.
 *
 * @param T the type of the extra value. Must be one of the supported types.
 * @param id the unique identifier for the extra. Use 0 for creating a new extra. Default value is 0.
 * @param actionId the identifier of the intent action for this extra.
 * @param key the key of the extra.
 * @param value the value of the extra.
 */
data class IntentExtra<T>(
    val id: Identifier,
    val actionId: Identifier,
    val key: String?,
    val value: T?,
) {

    /** @return true if this extra is complete and can be transformed into its entity. */
    fun isComplete(): Boolean = key != null && value != null

    /**
     * Copy and change the type of the value contained in this IntentExtra.
     * @param V the new value type.
     * @param value the new value.
     */
    fun <V> changeType(value: V): IntentExtra<V> {
        if (value !is Boolean && value !is Byte && value !is Char && value !is Double && value !is Int &&
            value !is Float && value !is Short && value !is String) {
            throw IllegalArgumentException("Unsupported value type")
        }

        return IntentExtra(id = id, actionId = actionId, key = key, value = value)
    }
}

/**
 * Add the provided intent extra into the Android intent.
 * @param extra the extra to be added.
 */
fun Intent.putDomainExtra(extra: IntentExtra<out Any>): Intent {
    when (val value = extra.value) {
        is Byte -> putExtra(extra.key, value)
        is Boolean -> putExtra(extra.key, value)
        is Char -> putExtra(extra.key, value)
        is Double -> putExtra(extra.key, value)
        is Int -> putExtra(extra.key, value)
        is Float -> putExtra(extra.key, value)
        is Short -> putExtra(extra.key, value)
        is String -> putExtra(extra.key, value)
        else -> throw IllegalArgumentException("Unsupported value type")
    }
    return this
}