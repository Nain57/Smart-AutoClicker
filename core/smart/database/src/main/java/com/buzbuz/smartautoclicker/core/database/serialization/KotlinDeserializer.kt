
package com.buzbuz.smartautoclicker.core.database.serialization

import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

internal class KotlinDeserializer : Deserializer {

    override fun deserializeCompleteScenario(jsonCompleteScenario: JsonObject): CompleteScenario =
        Json.decodeFromJsonElement(jsonCompleteScenario)
}