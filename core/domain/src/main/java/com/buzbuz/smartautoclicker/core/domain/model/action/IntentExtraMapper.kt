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

import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraType
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/** @return the entity equivalent of this intent extra. */
internal fun <T> IntentExtra<T>.toEntity(): IntentExtraEntity {
    if (key == null || value == null)
        throw IllegalStateException("Can't create entity, action is invalid")

    return IntentExtraEntity(
        id = id.databaseId,
        actionId = actionId.databaseId,
        type = value.toIntentExtraType(),
        key = key,
        value = value.toString(),
    )
}

/** @return the intent extra for this entity. */
internal fun IntentExtraEntity.toIntentExtra(asDomain: Boolean = false) = when (type) {
    IntentExtraType.BYTE -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value.toByte())
    IntentExtraType.BOOLEAN -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value.toBooleanStrict())
    IntentExtraType.CHAR -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value[0])
    IntentExtraType.DOUBLE -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value.toDouble())
    IntentExtraType.INTEGER -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value.toInt())
    IntentExtraType.FLOAT -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value.toFloat())
    IntentExtraType.SHORT -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value.toShort())
    IntentExtraType.STRING -> IntentExtra(Identifier(id, asDomain), Identifier(actionId, asDomain), key, value)
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