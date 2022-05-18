/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.domain

import android.content.Intent
import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraType

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
    var id: Long = 0,
    var actionId: Long,
    var key: String?,
    var value: T?,
) {

    /** @return the entity equivalent of this intent extra. */
    internal fun toEntity(): IntentExtraEntity {
        if (key == null || value == null)
            throw IllegalStateException("Can't create entity, action is invalid")

        val type = when (value) {
            is Boolean -> IntentExtraType.BOOLEAN
            is Byte -> IntentExtraType.BYTE
            is Char -> IntentExtraType.CHAR
            is Double -> IntentExtraType.DOUBLE
            is Int -> IntentExtraType.INTEGER
            is Float -> IntentExtraType.FLOAT
            is Short -> IntentExtraType.SHORT
            is String -> IntentExtraType.STRING
            else -> throw IllegalArgumentException("Unsupported value type")
        }
        return IntentExtraEntity(id, actionId, type, key!!, value!!.toString())
    }

    /** Cleanup all ids contained in this intent extra. Ideal for copying. */
    internal fun cleanUpIds() {
        id = 0
        actionId = 0
    }

    /**
     * Copy and change the type of the value contained in this IntentExtra.
     * @param V the new value type.
     * @param value the new value.
     */
    fun <V> copy(value: V): IntentExtra<V> {
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
fun Intent.putExtra(extra: IntentExtra<out Any>): Intent {
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

/** @return the intent extra for this entity. */
internal fun IntentExtraEntity.toIntentExtra() = when (type) {
    IntentExtraType.BYTE -> IntentExtra(id, actionId, key, value.toByte())
    IntentExtraType.BOOLEAN -> IntentExtra(id, actionId, key, value.toBooleanStrict())
    IntentExtraType.CHAR -> IntentExtra(id, actionId, key, value[0])
    IntentExtraType.DOUBLE -> IntentExtra(id, actionId, key, value.toDouble())
    IntentExtraType.INTEGER -> IntentExtra(id, actionId, key, value.toInt())
    IntentExtraType.FLOAT -> IntentExtra(id, actionId, key, value.toFloat())
    IntentExtraType.SHORT -> IntentExtra(id, actionId, key, value.toShort())
    IntentExtraType.STRING -> IntentExtra(id, actionId, key, value)
}