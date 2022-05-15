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
package com.buzbuz.smartautoclicker.database.backup

import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.database.backup.ext.getBoolean
import com.buzbuz.smartautoclicker.database.backup.ext.getEnumFromString
import com.buzbuz.smartautoclicker.database.backup.ext.getInt
import com.buzbuz.smartautoclicker.database.backup.ext.getJsonArray
import com.buzbuz.smartautoclicker.database.backup.ext.getJsonObject
import com.buzbuz.smartautoclicker.database.backup.ext.getLong
import com.buzbuz.smartautoclicker.database.backup.ext.getString
import com.buzbuz.smartautoclicker.database.domain.AND
import com.buzbuz.smartautoclicker.database.domain.EXACT
import com.buzbuz.smartautoclicker.database.domain.OR
import com.buzbuz.smartautoclicker.database.domain.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.database.room.CLICK_DATABASE_VERSION
import com.buzbuz.smartautoclicker.database.room.entity.ActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ActionType
import com.buzbuz.smartautoclicker.database.room.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteScenario
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EventEntity
import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraType
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioEntity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonObject

import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer/Deserializer for database scenarios (json).
 * It (tries to) handles the compatibility by deserializing manually if the version isn't the same.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ScenarioSerializer {

    /**
     * Serialize a scenario.
     *
     * @param scenario the scenario to serialize.
     * @param screenSize this device screen size.
     * @param outputStream the stream to serialize into.
     */
    fun serialize(scenario: CompleteScenario, screenSize: Point, outputStream: OutputStream) =
        Json.encodeToStream(ScenarioBackup(CLICK_DATABASE_VERSION, screenSize.x, screenSize.y, scenario), outputStream)

    /**
     * Deserialize a scenario.
     * Depending of the detected version, either kotlin or compat serialization will be used.
     *
     * @param json the stream to deserialize from.
     *
     * @return the scenario backup deserialized from the json.
     */
    fun deserialize(json: InputStream): ScenarioBackup? {
        val jsonBackup = Json.parseToJsonElement(json.readBytes().toString(Charsets.UTF_8)).jsonObject
        val version = jsonBackup.getInt("version", true) ?: -1
        val scenario = when {
            version < 8 -> {
                Log.w(TAG, "Can't deserialize scenario, invalid version.")
                null
            }
            version == CLICK_DATABASE_VERSION -> {
                Log.d(TAG, "Current version, use standard serialization.")
                Json.decodeFromJsonElement<ScenarioBackup>(jsonBackup).scenario
            }
            else -> {
                Log.d(TAG, "Not the current version, use compat serialization.")
                jsonBackup.deserializeCompleteScenarioCompat()
            }
        }

        if (scenario == null) {
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

    /**
     * Manual deserialization called when the version differs.
     * Tries to do a "best effort" deserialization of the provided json in order to keep as much backward
     * compatibility as possible.
     *
     * @return the complete scenario, if the deserialization is a success, or null if not.
     */
    private fun JsonObject.deserializeCompleteScenarioCompat(): CompleteScenario? {
        val jsonCompleteScenario = getJsonObject("scenario") ?: return null

        val scenario: ScenarioEntity = jsonCompleteScenario.getJsonObject("scenario")?.deserializeScenarioCompat()
            ?: return null

        return CompleteScenario(
            scenario = scenario,
            events =  jsonCompleteScenario.getJsonArray("events")?.deserializeCompleteEventCompat()
                ?: emptyList(),
            endConditions = jsonCompleteScenario.getJsonArray("endConditions")?.deserializeEndConditionsCompat()
                ?: emptyList()
        )
    }

    /** @return the deserialized scenario. */
    private fun JsonObject.deserializeScenarioCompat(): ScenarioEntity? {
        val id = getLong("id", true) ?: return null

        return ScenarioEntity(
            id = id,
            name = getString("name") ?: "",
            detectionQuality = getInt("detectionQuality") ?: 600,
            endConditionOperator = getInt("endConditionOperator") ?: OR,
        )
    }

    /** @return the deserialized end condition list. */
    private fun JsonArray.deserializeEndConditionsCompat(): List<EndConditionEntity> = mapNotNull { endCondition ->
        with (endCondition.jsonObject) {
            val id = getLong("id", true) ?: return@mapNotNull null
            val scenarioId = getLong("scenarioId", true) ?: return@mapNotNull null
            val eventId = getLong("eventId", true) ?: return@mapNotNull null

            EndConditionEntity(
                id = id,
                scenarioId = scenarioId,
                eventId = eventId,
                executions = getInt("executions") ?: 1
            )
        }
    }

    /** @return the deserialized complete event list. */
    private fun JsonArray.deserializeCompleteEventCompat(): List<CompleteEventEntity> = mapNotNull { completeEvent ->
        with (completeEvent.jsonObject) {
            val event = getJsonObject("event", true)?.deserializeEventCompat()
                ?: return@mapNotNull null

            val conditions = getJsonArray("conditions")?.deserializeConditionsCompat()
            if (conditions.isNullOrEmpty()) {
                Log.i(TAG, "Can't deserialize this complete event, there is no conditions")
                return@mapNotNull null
            }

            val completeActions = getJsonArray("actions")?.deserializeCompleteActionsCompat()
            if (completeActions.isNullOrEmpty()) {
                Log.i(TAG, "Can't deserialize this complete event, there is no actions")
                return@mapNotNull null
            }

            CompleteEventEntity(event, completeActions, conditions)
        }
    }

    /** @return the deserialized event. */
    private fun JsonObject.deserializeEventCompat(): EventEntity? {
        val id = getLong("id", true) ?: return null
        val scenarioId = getLong("scenarioId", true) ?: return null

        return EventEntity(
            id = id,
            scenarioId = scenarioId,
            name = getString("name") ?: "",
            conditionOperator = getInt("conditionOperator")?.coerceIn(AND, OR) ?: AND,
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            stopAfter = getInt("stopAfter")?.coerceAtLeast(0) ?: 0,
        )
    }

    /** @return the deserialized condition list. */
    private fun JsonArray.deserializeConditionsCompat(): List<ConditionEntity> = mapNotNull { condition ->
        with (condition.jsonObject) {
            val id = getLong("id", true) ?: return@mapNotNull null
            val eventId = getLong("eventId", true) ?: return@mapNotNull null
            val path = getString("path", true) ?: return@mapNotNull null
            val areaLeft = getInt("areaLeft", true) ?: return@mapNotNull null
            val areaTop = getInt("areaTop", true) ?: return@mapNotNull null
            val areaRight = getInt("areaRight", true) ?: return@mapNotNull null
            val areaBottom = getInt("areaBottom", true) ?: return@mapNotNull null

            ConditionEntity(
                id = id,
                eventId = eventId,
                path = path,
                areaLeft = areaLeft,
                areaTop = areaTop,
                areaRight = areaRight,
                areaBottom = areaBottom,
                name = getString("name") ?: "",
                shouldBeDetected = getBoolean("shouldBeDetected") ?: true,
                detectionType = getInt("detectionType")?.coerceIn(EXACT, WHOLE_SCREEN) ?: EXACT,
                threshold = getInt("threshold")?.coerceIn(0, 20) ?: 4,
            )
        }
    }

    /** @return the deserialized complete action list. */
    private fun JsonArray.deserializeCompleteActionsCompat(): List<CompleteActionEntity> = mapNotNull { completeActions ->
        with (completeActions.jsonObject) {
            val action = getJsonObject("action")?.deserializeActionCompat() ?: return@mapNotNull null

            CompleteActionEntity(
                action = action,
                intentExtras = getJsonArray("intentExtras")?.deserializeIntentExtrasCompat() ?: emptyList()
            )
        }
    }

    /** @return the deserialized action. */
    private fun JsonObject.deserializeActionCompat(): ActionEntity? =
        when (getEnumFromString<ActionType>("type", true)) {
            ActionType.CLICK -> deserializeClickActionCompat()
            ActionType.SWIPE -> deserializeSwipeActionCompat()
            ActionType.PAUSE -> deserializePauseActionCompat()
            ActionType.INTENT -> deserializeIntentActionCompat()
            null -> null
        }

    /** @return the deserialized click action. */
    private fun JsonObject.deserializeClickActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        val x: Int?
        val y: Int?
        val clickOnCondition = getBoolean("clickOnCondition") ?: false
        if (clickOnCondition) {
            x = null
            y = null
        } else {
            x = getInt("x", true) ?: return null
            y = getInt("y", true) ?: return null
        }

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority") ?: 0,
            type = ActionType.CLICK,
            clickOnCondition = clickOnCondition,
            x = x,
            y = y,
            pressDuration = getLong("pressDuration") ?: 0,
        )
    }

    /** @return the deserialized swipe action. */
    private fun JsonObject.deserializeSwipeActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        val fromX = getInt("fromX", true) ?: return null
        val fromY = getInt("fromY", true) ?: return null
        val toX = getInt("toX", true) ?: return null
        val toY = getInt("toY", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority") ?: 0,
            type = ActionType.SWIPE,
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            swipeDuration = getLong("swipeDuration") ?: 250,
        )
    }

    /** @return the deserialized pause action. */
    private fun JsonObject.deserializePauseActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority") ?: 0,
            type = ActionType.PAUSE,
            pauseDuration = getLong("pauseDuration") ?: 50,
        )
    }

    /** @return the deserialized intent action. */
    private fun JsonObject.deserializeIntentActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null
        val intentAction = getString("intent_action", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority") ?: 0,
            type = ActionType.INTENT,
            isAdvanced = getBoolean("isAdvanced") ?: false,
            isBroadcast = getBoolean("isBroadcast") ?: false,
            intentAction = intentAction,
            componentName = getString("component_name"),
            flags = getInt("flags") ?: 0,
        )
    }

    /** @return the deserialized intent extra. */
    private fun JsonArray.deserializeIntentExtrasCompat(): List<IntentExtraEntity> = mapNotNull { extra ->
        with (extra.jsonObject) {
            val id = getLong("id", true) ?: return@mapNotNull null
            val actionId = getLong("action_id", true) ?: return@mapNotNull null
            val type = getEnumFromString<IntentExtraType>("type", true) ?: return@mapNotNull null
            val key = getString("key", true) ?: return@mapNotNull null
            val value = getString("value", true) ?: return@mapNotNull null

            IntentExtraEntity(id, actionId, type, key, value)
        }
    }
}

/** Tag for logs. */
private const val TAG = "ScenarioDeserializer"