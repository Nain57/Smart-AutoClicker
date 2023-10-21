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
package com.buzbuz.smartautoclicker.feature.backup.data.smart

import android.util.Log
import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.feature.backup.data.ext.getBoolean
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getEnum
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getInt
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getJsonArray
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getJsonObject
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getLong
import com.buzbuz.smartautoclicker.feature.backup.data.ext.getString
import com.buzbuz.smartautoclicker.core.database.CLICK_DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.database.entity.*
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupSerializer

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
                Log.d(TAG, "$version is not the current, use compat serialization.")
                jsonBackup.deserializeCompleteScenarioCompat(version)
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
     * @param version the version of the scenario to extract.
     *
     * @return the complete scenario, if the deserialization is a success, or null if not.
     */
    @VisibleForTesting
    internal fun JsonObject.deserializeCompleteScenarioCompat(version: Int): CompleteScenario? {
        val jsonCompleteScenario = getJsonObject("scenario") ?: return null

        val scenario: ScenarioEntity = jsonCompleteScenario.getJsonObject("scenario")
            ?.deserializeScenarioCompat(version)
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
    @VisibleForTesting
    internal fun JsonObject.deserializeScenarioCompat(version: Int): ScenarioEntity? {
        val id = getLong("id", true) ?: return null

        val detectionQuality = getInt("detectionQuality")?.let { quality ->
            if (version < DETECTION_QUALITY_UPDATE_VERSION) quality + 600
            else quality
        } ?: DETECTION_QUALITY_DEFAULT_VALUE

        return ScenarioEntity(
            id = id,
            name = getString("name") ?: "",
            detectionQuality = detectionQuality.coerceIn(DETECTION_QUALITY_LOWER_BOUND, DETECTION_QUALITY_UPPER_BOUND),
            endConditionOperator = getInt("endConditionOperator")
                ?.coerceIn(OPERATOR_LOWER_BOUND, OPERATOR_UPPER_BOUND)
                ?: OPERATOR_DEFAULT_VALUE,
            randomize = getBoolean("randomize") ?: false,
        )
    }

    /** @return the deserialized end condition list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeEndConditionsCompat(): List<EndConditionEntity> = mapNotNull { endCondition ->
        with (endCondition.jsonObject) {
            val id = getLong("id", true) ?: return@mapNotNull null
            val scenarioId = getLong("scenarioId", true) ?: return@mapNotNull null
            val eventId = getLong("eventId", true) ?: return@mapNotNull null

            EndConditionEntity(
                id = id,
                scenarioId = scenarioId,
                eventId = eventId,
                executions = getInt("executions") ?: END_CONDITION_EXECUTION_DEFAULT_VALUE
            )
        }
    }

    /** @return the deserialized complete event list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeCompleteEventCompat(): List<CompleteEventEntity> = mapNotNull { completeEvent ->
        with (completeEvent.jsonObject) {
            val event = getJsonObject("event", true)?.deserializeEventCompat()
                ?: return@mapNotNull null

            val conditions = getJsonArray("conditions")?.deserializeConditionsCompat()
            if (conditions.isNullOrEmpty()) {
                Log.i(TAG, "Can't deserialize this complete event, there is no conditions")
                return@mapNotNull null
            }

            val completeActions = getJsonArray("actions")?.deserializeCompleteActionsCompat(conditions)
            if (completeActions.isNullOrEmpty()) {
                Log.i(TAG, "Can't deserialize this complete event, there is no actions")
                return@mapNotNull null
            }

            CompleteEventEntity(event, completeActions, conditions)
        }
    }

    /** @return the deserialized event. */
    @VisibleForTesting
    internal fun JsonObject.deserializeEventCompat(): EventEntity? {
        val id = getLong("id", true) ?: return null
        val scenarioId = getLong("scenarioId", true) ?: return null

        return EventEntity(
            id = id,
            scenarioId = scenarioId,
            name = getString("name") ?: "",
            conditionOperator = getInt("conditionOperator")
                ?.coerceIn(OPERATOR_LOWER_BOUND, OPERATOR_UPPER_BOUND)
                ?: OPERATOR_DEFAULT_VALUE,
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            enabledOnStart = getBoolean("enabledOnStart") ?: true,
        )
    }

    /** @return the deserialized condition list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeConditionsCompat(): List<ConditionEntity> = mapNotNull { condition ->
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
                detectionType = getInt("detectionType")
                    ?.coerceIn(DETECTION_TYPE_LOWER_BOUND, DETECTION_TYPE_UPPER_BOUND)
                    ?: DETECTION_TYPE_DEFAULT_VALUE,
                threshold = getInt("threshold")
                    ?.coerceIn(CONDITION_THRESHOLD_LOWER_BOUND, CONDITION_THRESHOLD_UPPER_BOUND)
                    ?: CONDITION_THRESHOLD_DEFAULT_VALUE,
                detectionAreaLeft = getInt("detectionAreaLeft"),
                detectionAreaTop = getInt("detectionAreaTop"),
                detectionAreaRight = getInt("detectionAreaRight"),
                detectionAreaBottom = getInt("detectionAreaBottom"),
            )
        }
    }

    /** @return the deserialized complete action list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeCompleteActionsCompat(conditions: List<ConditionEntity>): List<CompleteActionEntity> = mapNotNull { completeActions ->
        with (completeActions.jsonObject) {
            val action = getJsonObject("action")?.deserializeActionCompat(conditions)
                ?: return@mapNotNull null

            CompleteActionEntity(
                action = action,
                intentExtras = getJsonArray("intentExtras")?.deserializeIntentExtrasCompat() ?: emptyList()
            )
        }
    }

    /** @return the deserialized action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeActionCompat(conditions: List<ConditionEntity>): ActionEntity? =
        when (getEnum<ActionType>("type", true)) {
            ActionType.CLICK -> deserializeClickActionCompat(conditions)
            ActionType.SWIPE -> deserializeSwipeActionCompat()
            ActionType.PAUSE -> deserializePauseActionCompat()
            ActionType.INTENT -> deserializeIntentActionCompat()
            ActionType.TOGGLE_EVENT -> deserializeToggleEventActionCompat()
            else -> null
        }

    /** @return the deserialized click action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeClickActionCompat(conditions: List<ConditionEntity>): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        val x: Int?
        val y: Int?
        val clickOnConditionId: Long?
        val clickPositionType: ClickPositionType

        // Before v11, they were no condition attached when selecting this option. Starting with v11,
        // condition id is attached and position type is clearly defined
        val clickOnCondition = getBoolean("clickOnCondition")
        if (clickOnCondition != null) {
            if (clickOnCondition) {
                x = null
                y = null
                clickOnConditionId = conditions.find { it.shouldBeDetected }?.id
                clickPositionType = ClickPositionType.ON_DETECTED_CONDITION
            } else {
                x = getInt("x", true) ?: return null
                y = getInt("y", true) ?: return null
                clickOnConditionId = null
                clickPositionType = ClickPositionType.USER_SELECTED
            }
        } else {
            clickPositionType = getEnum<ClickPositionType>("clickPositionType", true) ?: return null

            when (clickPositionType) {
                ClickPositionType.ON_DETECTED_CONDITION -> {
                    x = null
                    y = null
                    clickOnConditionId = getLong("clickOnConditionId", true) ?: return null
                    if (conditions.find { it.id == clickOnConditionId } == null) {
                        Log.w(TAG, "Can't deserialize action, clickOnConditionId is not valid.")
                        return null
                    }
                }

                ClickPositionType.USER_SELECTED -> {
                    x = getInt("x", true) ?: return null
                    y = getInt("y", true) ?: return null
                    clickOnConditionId = null
                }
            }
        }

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.CLICK,
            clickPositionType = clickPositionType,
            clickOnConditionId = clickOnConditionId,
            x = x,
            y = y,
            pressDuration = getLong("pressDuration")
                ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
                ?: DEFAULT_CLICK_DURATION,
        )
    }

    /** @return the deserialized swipe action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeSwipeActionCompat(): ActionEntity? {
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
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.SWIPE,
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            swipeDuration = getLong("swipeDuration")
                ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
                ?: DEFAULT_SWIPE_DURATION,
        )
    }

    /** @return the deserialized pause action. */
    @VisibleForTesting
    internal fun JsonObject.deserializePauseActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.PAUSE,
            pauseDuration = getLong("pauseDuration")?.coerceAtLeast(0) ?: DEFAULT_PAUSE_DURATION,
        )
    }

    /** @return the deserialized intent action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeIntentActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null
        val intentAction = getString("intentAction", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.INTENT,
            isAdvanced = getBoolean("isAdvanced") ?: false,
            isBroadcast = getBoolean("isBroadcast") ?: false,
            intentAction = intentAction,
            componentName = getString("componentName"),
            flags = getInt("flags")?.coerceAtLeast(0) ?: 0,
        )
    }

    /** @return the deserialized toggle event action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeToggleEventActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null
        val toggleEventId = getLong("toggleEventId") ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.TOGGLE_EVENT,
            toggleEventId = toggleEventId,
            toggleEventType = getEnum<ToggleEventType>("toggleEventType"),
        )
    }

    /** @return the deserialized intent extra. */
    @VisibleForTesting
    internal fun JsonArray.deserializeIntentExtrasCompat(): List<IntentExtraEntity> = mapNotNull { extra ->
        with (extra.jsonObject) {
            val id = getLong("id", true) ?: return@mapNotNull null
            val actionId = getLong("actionId", true) ?: return@mapNotNull null
            val type = getEnum<IntentExtraType>("type", true) ?: return@mapNotNull null
            val key = getString("key", true) ?: return@mapNotNull null
            val value = getString("value", true) ?: return@mapNotNull null

            IntentExtraEntity(id, actionId, type, key, value)
        }
    }
}

/** Scenario detection quality revision update version. */
const val DETECTION_QUALITY_UPDATE_VERSION = 11
/** Scenario detection quality lower bound on compat deserialization. */
const val DETECTION_QUALITY_LOWER_BOUND = 400
/** Scenario detection quality upper bound on compat deserialization. */
const val DETECTION_QUALITY_UPPER_BOUND = 3216
/** Scenario detection quality default value on compat deserialization. */
const val DETECTION_QUALITY_DEFAULT_VALUE = 1200

/** Operators lower bound on compat deserialization. */
const val OPERATOR_LOWER_BOUND = 1
/** Operators upper bound on compat deserialization. */
const val OPERATOR_UPPER_BOUND = 2
/** Operators default value on compat deserialization. */
const val OPERATOR_DEFAULT_VALUE = OPERATOR_LOWER_BOUND

/** Detection type lower bound on compat deserialization. */
const val DETECTION_TYPE_LOWER_BOUND = 1
/** Detection type upper bound on compat deserialization. */
const val DETECTION_TYPE_UPPER_BOUND = 2
/** Detection type default value on compat deserialization. */
const val DETECTION_TYPE_DEFAULT_VALUE = DETECTION_TYPE_LOWER_BOUND

/** Condition threshold lower bound on compat deserialization. */
const val CONDITION_THRESHOLD_LOWER_BOUND = 0
/** Condition threshold upper bound on compat deserialization. */
const val CONDITION_THRESHOLD_UPPER_BOUND = 20
/** Condition threshold default value on compat deserialization. */
const val CONDITION_THRESHOLD_DEFAULT_VALUE = 4

/** End condition executions default value on compat deserialization. */
const val END_CONDITION_EXECUTION_DEFAULT_VALUE = 1

/** The minimum value for all durations. */
const val DURATION_LOWER_BOUND = 1L
/** The maximum value for all gestures durations. */
const val DURATION_GESTURE_UPPER_BOUND = 59_999L
/** Default click duration in ms on compat deserialization. */
const val DEFAULT_CLICK_DURATION = 1L
/** Default swipe duration in ms on compat deserialization. */
const val DEFAULT_SWIPE_DURATION = 250L
/** Default pause duration in ms on compat deserialization. */
const val DEFAULT_PAUSE_DURATION = 50L

/** Tag for logs. */
private const val TAG = "ScenarioDeserializer"