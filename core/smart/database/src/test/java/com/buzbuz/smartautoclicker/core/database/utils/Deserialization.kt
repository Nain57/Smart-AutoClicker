
package com.buzbuz.smartautoclicker.core.database.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

internal val NULL_BOOLEAN_JSON_PRIMITIVE = JsonPrimitive(null as Boolean?)
internal val NULL_NUMBER_JSON_PRIMITIVE = JsonPrimitive(null as Long?)
internal val NULL_STRING_JSON_PRIMITIVE = JsonPrimitive(null as String?)

internal inline fun <reified T: Any> T.encodeToJsonObject(): JsonObject =
    Json.encodeToJsonElement(this).jsonObject

internal fun List<JsonObject>.toJsonArray(): JsonArray =
    JsonArray(this)