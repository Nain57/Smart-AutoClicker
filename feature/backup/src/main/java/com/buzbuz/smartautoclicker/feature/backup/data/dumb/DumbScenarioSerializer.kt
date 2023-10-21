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
package com.buzbuz.smartautoclicker.feature.backup.data.dumb

import android.util.Log

import com.buzbuz.smartautoclicker.core.dumb.data.database.DUMB_DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DUMB_SCENARIO_MAX_DURATION_MINUTES
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DUMB_SCENARIO_MIN_DURATION_MINUTES
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MAX_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MIN_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_DELAY_MAX_MS
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_DELAY_MIN_MS
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupSerializer
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getBoolean
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getEnum
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getInt
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getJsonArray
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getJsonObject
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getLong
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getString

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonObject

import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal class DumbScenarioSerializer : ScenarioBackupSerializer<DumbScenarioBackup> {

    /**
     * Serialize a dumb scenario.
     *
     * @param scenarioBackup the scenario to serialize.
     * @param outputStream the stream to serialize into.
     */
    override fun serialize(scenarioBackup: DumbScenarioBackup, outputStream: OutputStream) {
        Json.encodeToStream(scenarioBackup, outputStream)
    }

    /**
     * Deserialize a dumb scenario.
     * Depending of the detected version, either kotlin or compat serialization will be used.
     *
     * @param json the stream to deserialize from.
     *
     * @return the scenario backup deserialized from the json.
     */
    override fun deserialize(json: InputStream): DumbScenarioBackup? {
        Log.d(TAG, "Deserializing dumb scenario")

        val jsonBackup = Json.parseToJsonElement(json.readBytes().toString(Charsets.UTF_8)).jsonObject
        val version = jsonBackup.getInt("version", true) ?: -1

        val scenario = when {
            version < 1 -> {
                Log.w(TAG, "Can't deserialize dumb scenario, invalid version.")
                null
            }
            version == DUMB_DATABASE_VERSION -> {
                Log.d(TAG, "Current version, use standard serialization.")
                Json.decodeFromJsonElement<DumbScenarioBackup>(jsonBackup).dumbScenario
            }
            else -> {
                Log.d(TAG, "$version is not the current version, use compat serialization.")
                jsonBackup.deserializeCompleteDumbScenarioCompat()
            }
        }

        if (scenario == null) {
            Log.w(TAG, "Can't deserialize dumb scenario.")
            return null
        }

        return DumbScenarioBackup(
            version = version,
            screenWidth = jsonBackup.getInt("screenWidth") ?: 0,
            screenHeight = jsonBackup.getInt("screenHeight") ?: 0,
            dumbScenario = scenario,
        )
    }

    private fun JsonObject.deserializeCompleteDumbScenarioCompat(): DumbScenarioWithActions? {
        val jsonCompleteDumbScenario = getJsonObject("dumbScenario") ?: return null

        val scenario: DumbScenarioEntity = jsonCompleteDumbScenario.getJsonObject("scenario")
            ?.deserializeDumbScenarioCompat()
            ?: return null

        return DumbScenarioWithActions(
            scenario = scenario,
            dumbActions = jsonCompleteDumbScenario.getJsonArray("dumbActions")
                ?.deserializeDumbActionsCompat()
                ?: return null
        )
    }

    private fun JsonObject.deserializeDumbScenarioCompat() : DumbScenarioEntity? {
        val id = getLong("id", true) ?: return null

        val name = getString("name", true)
        if (name.isNullOrEmpty()) return null

        return DumbScenarioEntity(
            id = id,
            name = name,
            repeatCount = getInt("repeatCount")
                ?.coerceIn(REPEAT_COUNT_MIN_VALUE..REPEAT_COUNT_MAX_VALUE)
                ?: DEFAULT_DUMB_REPEAT_COUNT,
            isRepeatInfinite = getBoolean("isRepeatInfinite") ?: DEFAULT_DUMB_REPEAT_IS_INFINITE,
            maxDurationMin = getInt("maxDurationMin")
                ?.coerceIn(DUMB_SCENARIO_MIN_DURATION_MINUTES..DUMB_SCENARIO_MAX_DURATION_MINUTES)
                ?: DEFAULT_DUMB_MAX_DURATION_MINUTES,
            isDurationInfinite = getBoolean("isDurationInfinite") ?: DEFAULT_DUMB_DURATION_IS_INFINITE,
            randomize = getBoolean("randomize") ?: DEFAULT_DUMB_RANDOMIZE,
        )
    }

    private fun JsonArray.deserializeDumbActionsCompat(): List<DumbActionEntity> =
        mapNotNull { jsonElement ->
            jsonElement.getJsonObject()?.let { jsonDumbAction ->
                when (jsonDumbAction.getEnum<DumbActionType>("type", true)) {
                    DumbActionType.CLICK -> jsonDumbAction.deserializeDumbClickCompat()
                    DumbActionType.SWIPE -> jsonDumbAction.deserializeDumbSwipeCompat()
                    DumbActionType.PAUSE -> jsonDumbAction.deserializeDumbPauseCompat()
                    else -> null
                }
            }
        }

    private fun JsonObject.deserializeDumbClickCompat(): DumbActionEntity? {
        val id = getLong("id", true) ?: return null
        val scenarioId = getLong("dumb_scenario_id", true) ?: return null

        val name = getString("name", true)
        if (name.isNullOrEmpty()) return null

        val x = getInt("x", true) ?: return null
        val y = getInt("y", true) ?: return null

        return DumbActionEntity(
            id = id,
            dumbScenarioId = scenarioId,
            name = name,
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = DumbActionType.CLICK,
            x = x,
            y = y,
            pressDuration = getLong("pressDuration")
                ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
                ?: DEFAULT_DUMB_CLICK_DURATION,
            repeatCount = getInt("repeatCount")
                ?.coerceIn(REPEAT_COUNT_MIN_VALUE..REPEAT_COUNT_MAX_VALUE)
                ?: DEFAULT_DUMB_REPEAT_COUNT,
            isRepeatInfinite = getBoolean("isRepeatInfinite") ?: DEFAULT_DUMB_REPEAT_IS_INFINITE,
            repeatDelay = getLong("repeatDelay")
                ?.coerceIn(REPEAT_DELAY_MIN_MS.. REPEAT_DELAY_MAX_MS)
                ?: DEFAULT_DUMB_REPEAT_DELAY_MS,
        )
    }

    private fun JsonObject.deserializeDumbSwipeCompat(): DumbActionEntity? {
        val id = getLong("id", true) ?: return null
        val scenarioId = getLong("dumb_scenario_id", true) ?: return null

        val name = getString("name", true)
        if (name.isNullOrEmpty()) return null

        val fromX = getInt("fromX", true) ?: return null
        val fromY = getInt("fromY", true) ?: return null
        val toX = getInt("toX", true) ?: return null
        val toY = getInt("toY", true) ?: return null

        return DumbActionEntity(
            id = id,
            dumbScenarioId = scenarioId,
            name = name,
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = DumbActionType.SWIPE,
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            swipeDuration = getLong("swipeDuration")
                ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
                ?: DEFAULT_DUMB_SWIPE_DURATION,
            repeatCount = getInt("repeatCount")
                ?.coerceIn(REPEAT_COUNT_MIN_VALUE..REPEAT_COUNT_MAX_VALUE)
                ?: DEFAULT_DUMB_REPEAT_COUNT,
            isRepeatInfinite = getBoolean("isRepeatInfinite") ?: DEFAULT_DUMB_REPEAT_IS_INFINITE,
            repeatDelay = getLong("repeatDelay")
                ?.coerceIn(REPEAT_DELAY_MIN_MS.. REPEAT_DELAY_MAX_MS)
                ?: DEFAULT_DUMB_REPEAT_DELAY_MS,
        )
    }

    private fun JsonObject.deserializeDumbPauseCompat(): DumbActionEntity? {
        val id = getLong("id", true) ?: return null
        val scenarioId = getLong("dumb_scenario_id", true) ?: return null

        val name = getString("name", true)
        if (name.isNullOrEmpty()) return null

        return DumbActionEntity(
            id = id,
            dumbScenarioId = scenarioId,
            name = name,
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = DumbActionType.PAUSE,
            pauseDuration = getLong("pauseDuration")
                ?.coerceAtLeast(0)
                ?: DEFAULT_DUMB_PAUSE_DURATION,
        )
    }
}

/** The minimum value for all durations. */
private const val DURATION_LOWER_BOUND = 1L
/** The maximum value for all gestures durations. */
private const val DURATION_GESTURE_UPPER_BOUND = 59_999L

/** Default dumb click duration in ms on compat deserialization. */
private const val DEFAULT_DUMB_CLICK_DURATION = 1L
/** Default dumb swipe duration in ms on compat deserialization. */
private const val DEFAULT_DUMB_SWIPE_DURATION = 250L
/** Default dumb pause duration in ms on compat deserialization. */
private const val DEFAULT_DUMB_PAUSE_DURATION = 50L

private const val DEFAULT_DUMB_REPEAT_COUNT = 1
private const val DEFAULT_DUMB_REPEAT_IS_INFINITE = false
private const val DEFAULT_DUMB_REPEAT_DELAY_MS = 1L
private const val DEFAULT_DUMB_MAX_DURATION_MINUTES = 1
private const val DEFAULT_DUMB_DURATION_IS_INFINITE = true
private const val DEFAULT_DUMB_RANDOMIZE = true

private const val TAG = "DumbScenarioSerializer"