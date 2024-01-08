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