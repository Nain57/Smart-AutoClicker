
package com.buzbuz.smartautoclicker.core.domain.model.action.intent

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
internal fun IntentExtraEntity.toDomainIntentExtra(cleanIds: Boolean = false) = when (type) {
    IntentExtraType.BYTE -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value.toByte())
    IntentExtraType.BOOLEAN -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value.toBooleanStrict())
    IntentExtraType.CHAR -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value[0])
    IntentExtraType.DOUBLE -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value.toDouble())
    IntentExtraType.INTEGER -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value.toInt())
    IntentExtraType.FLOAT -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value.toFloat())
    IntentExtraType.SHORT -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value.toShort())
    IntentExtraType.STRING -> IntentExtra(Identifier(id, cleanIds), Identifier(actionId, cleanIds), key, value)
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