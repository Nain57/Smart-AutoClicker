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
package com.buzbuz.smartautoclicker.domain.model.action

import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraType

/** @return the entity equivalent of this intent extra. */
internal fun <T> IntentExtra<T>.toEntity(): IntentExtraEntity {
    if (key == null || value == null)
        throw IllegalStateException("Can't create entity, action is invalid")

    return IntentExtraEntity(
        id,
        actionId,
        value.toIntentExtraType(),
        key!!,
        value!!.toString(),
    )
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

private fun <T> T?.toIntentExtraType(): IntentExtraType = when (this) {
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