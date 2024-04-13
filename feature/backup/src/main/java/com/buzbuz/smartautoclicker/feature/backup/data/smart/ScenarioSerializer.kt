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
package com.buzbuz.smartautoclicker.feature.backup.data.smart

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.extensions.getInt
import com.buzbuz.smartautoclicker.core.base.extensions.getJsonObject
import com.buzbuz.smartautoclicker.core.database.serialization.DeserializerFactory
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupSerializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonObject

import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer/Deserializer for database scenarios (json).
 * It (tries to) handles the compatibility by deserializing manually if the version isn't the same.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ScenarioSerializer : ScenarioBackupSerializer<ScenarioBackup> {

    /**
     * Serialize a scenario.
     *
     * @param scenarioBackup the scenario to serialize.
     * @param outputStream the stream to serialize into.
     */
    override fun serialize(scenarioBackup: ScenarioBackup, outputStream: OutputStream) =
        Json.encodeToStream(scenarioBackup, outputStream)

    /**
     * Deserialize a scenario.
     * Depending of the detected version, either kotlin or compat serialization will be used.
     *
     * @param json the stream to deserialize from.
     *
     * @return the scenario backup deserialized from the json.
     */
    override fun deserialize(json: InputStream): ScenarioBackup? {
        Log.d(TAG, "Deserializing smart scenario")

        val jsonBackup = Json.parseToJsonElement(json.readBytes().toString(Charsets.UTF_8)).jsonObject
        val version = jsonBackup.getInt("version", true) ?: -1

        val scenario = jsonBackup.getJsonObject("scenario", true)?.let { scenario ->
            DeserializerFactory.create(version)
                ?.deserializeCompleteScenario(scenario)
        } ?:let {
            Log.w(TAG, "Can't deserialize scenario.")
            return null
        }

        return ScenarioBackup(
            version = version,
            screenWidth = jsonBackup.getInt("screenWidth") ?: 0,
            screenHeight = jsonBackup.getInt("screenHeight") ?: 0,
            scenario = scenario,
        )
    }
}

/** Tag for logs. */
private const val TAG = "ScenarioDeserializer"