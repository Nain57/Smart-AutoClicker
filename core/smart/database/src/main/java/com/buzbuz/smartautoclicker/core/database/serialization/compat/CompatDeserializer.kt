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
package com.buzbuz.smartautoclicker.core.database.serialization.compat

import android.util.Log
import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.core.base.extensions.getBoolean
import com.buzbuz.smartautoclicker.core.base.extensions.getEnum
import com.buzbuz.smartautoclicker.core.base.extensions.getInt
import com.buzbuz.smartautoclicker.core.base.extensions.getJsonArray
import com.buzbuz.smartautoclicker.core.base.extensions.getJsonObject
import com.buzbuz.smartautoclicker.core.base.extensions.getLong
import com.buzbuz.smartautoclicker.core.base.extensions.getString
import com.buzbuz.smartautoclicker.core.base.extensions.getListOf
import com.buzbuz.smartautoclicker.core.base.extensions.getRect
import com.buzbuz.smartautoclicker.core.base.interfaces.containsId
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.database.entity.EventType
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraType
import com.buzbuz.smartautoclicker.core.database.entity.NotificationMessageType
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.serialization.Deserializer

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal open class CompatDeserializer : Deserializer {

    protected companion object {
        /** Scenario detection quality lower bound on compat deserialization. */
        const val DETECTION_QUALITY_LOWER_BOUND = 400
        /** Scenario detection quality upper bound on compat deserialization. */
        const val DETECTION_QUALITY_UPPER_BOUND = 10000
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

        /** Tag for logs */
        private const val TAG = "DeserializerCompat"
    }

    override fun deserializeCompleteScenario(jsonCompleteScenario: JsonObject): CompleteScenario {
        val scenarioEntity = jsonCompleteScenario.getJsonObject("scenario")?.let(::deserializeScenario)
            ?: throw IllegalArgumentException("Json CompleteScenario doesn't contains a valid Scenario")

        val jsonCompleteEvents = jsonCompleteScenario.getJsonArray("events")?.let { jsonEvents ->
            deserializeCompleteEvents(jsonEvents)
        } ?: emptyList()

        return CompleteScenario(
            scenario = scenarioEntity,
            events =  jsonCompleteEvents,
        )
    }

    open fun deserializeCompleteEvents(jsonCompleteEvents: JsonArray): List<CompleteEventEntity> {
        val eventEntityList = mutableListOf<EventEntity>()
        val eventMap = jsonCompleteEvents.mapNotNull { jsonCompleteEvent ->
            jsonCompleteEvent.jsonObject.getJsonObject("event", true)?.let { jsonEvent ->
                jsonEvent.let(::deserializeEvent)?.let { eventEntity ->
                    eventEntityList.add(eventEntity)
                    jsonCompleteEvent.jsonObject to eventEntity
                }
            }
        }

        return eventMap.mapNotNull { (jsonCompleteEvent, eventEntity) ->
            val conditions = jsonCompleteEvent.getJsonArray("conditions")
                ?.getListOf(::deserializeCondition)
            if (conditions.isNullOrEmpty()) {
                Log.w(TAG, "There is no conditions in this event")
                return@mapNotNull null
            }

            val completeActions = jsonCompleteEvent.jsonObject.getJsonArray("actions")
                ?.getListOf { jsonAction -> deserializeCompleteAction(
                    jsonAction,
                    eventEntityList,
                    conditions,
                    eventEntity.conditionOperator,
                )
            }
            if (completeActions.isNullOrEmpty()) {
                Log.w(TAG, "Can't deserialize this complete event, there is no actions")
                return@mapNotNull null
            }

            CompleteEventEntity(
                event = eventEntity,
                actions = completeActions,
                conditions = conditions,
            )
        }
    }

    open fun deserializeCompleteAction(
        jsonCompleteAction: JsonObject,
        scenarioEvents: List<EventEntity>,
        eventConditions: List<ConditionEntity>,
        conditionsOperator: Int,
    ): CompleteActionEntity? {
        val actionEntity = jsonCompleteAction.getJsonObject("action")?.let { jsonAction ->
            deserializeAction(jsonAction, eventConditions, conditionsOperator)
        } ?: return null

        val extraEntityList = jsonCompleteAction.getJsonArray("intentExtras")
            ?.getListOf(::deserializeIntentExtra)
            ?: emptyList()

        val eventToggleList = jsonCompleteAction.getJsonArray("eventsToggle")
            ?.getListOf { jsonEventToggle -> deserializeEventToggle(jsonEventToggle, scenarioEvents)}
            ?: emptyList()

        return CompleteActionEntity(
            action = actionEntity,
            intentExtras = extraEntityList,
            eventsToggle = eventToggleList,
        )
    }

    // ======================= SCENARIO

    /** @return the deserialized scenario. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeScenario(jsonScenario: JsonObject): ScenarioEntity? {
        val id = jsonScenario.getLong("id", true) ?: return null

        val detectionQuality = deserializeScenarioDetectionQuality(jsonScenario)
            .coerceIn(DETECTION_QUALITY_LOWER_BOUND, DETECTION_QUALITY_UPPER_BOUND)

        return ScenarioEntity(
            id = id,
            name = jsonScenario.getString("name") ?: "",
            detectionQuality = detectionQuality,
            randomize = jsonScenario.getBoolean("randomize") ?: false,
        )
    }

    /** @return the scenario detection quality. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeScenarioDetectionQuality(jsonScenario: JsonObject): Int =
        jsonScenario.getInt("detectionQuality") ?: DETECTION_QUALITY_DEFAULT_VALUE


    // ======================= EVENT

    open fun deserializeEvent(jsonEvent: JsonObject): EventEntity? {
        val id = jsonEvent.getLong("id", true) ?: return null
        val scenarioId = jsonEvent.getLong("scenarioId", true) ?: return null
        val type = deserializeEventType(jsonEvent) ?: return null

        val conditionOperator = jsonEvent.getInt("conditionOperator")
            ?.coerceIn(OPERATOR_LOWER_BOUND, OPERATOR_UPPER_BOUND)
            ?: OPERATOR_DEFAULT_VALUE

        val keepDetecting = jsonEvent.getBoolean("keepDetecting") ?: false

        return EventEntity(
            id = id,
            scenarioId = scenarioId,
            name = jsonEvent.getString("name") ?: "",
            conditionOperator = conditionOperator,
            priority = jsonEvent.getInt("priority")?.coerceAtLeast(0) ?: 0,
            enabledOnStart = jsonEvent.getBoolean("enabledOnStart") ?: true,
            type = type,
            keepDetecting = keepDetecting,
        )
    }

    /** @return the type of event. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeEventType(jsonEvent: JsonObject): EventType? =
        jsonEvent.getEnum<EventType>("type", shouldLogError = true)


    // ======================= CONDITION

    /** @return the deserialized condition list. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeCondition(jsonCondition: JsonObject): ConditionEntity? =
        when (deserializeConditionType(jsonCondition)) {
            ConditionType.ON_BROADCAST_RECEIVED -> deserializeConditionBroadcastReceived(jsonCondition)
            ConditionType.ON_COUNTER_REACHED -> deserializeConditionCounterReached(jsonCondition)
            ConditionType.ON_IMAGE_DETECTED -> deserializeConditionImageDetected(jsonCondition)
            ConditionType.ON_TEXT_DETECTED -> deserializeConditionTextDetected(jsonCondition)
            ConditionType.ON_TIMER_REACHED -> deserializeConditionTimerReached(jsonCondition)
            null -> null
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeConditionImageDetected(jsonCondition: JsonObject): ConditionEntity? {
        val id = jsonCondition.getLong("id", true) ?: return null
        val eventId = jsonCondition.getLong("eventId", true) ?: return null
        val area = jsonCondition.getRect("areaLeft", "areaTop", "areaRight", "areaBottom")
            ?: return null
        val path = jsonCondition.getString("path", true) ?: return null

        return ConditionEntity(
            id = id,
            eventId = eventId,
            name = jsonCondition.getString("name") ?: "",
            priority = jsonCondition.getInt("priority") ?: 0,
            type = ConditionType.ON_IMAGE_DETECTED,
            path = path,
            areaLeft = area.left,
            areaTop = area.top,
            areaRight = area.right,
            areaBottom = area.bottom,
            shouldBeDetected = jsonCondition.getBoolean("shouldBeDetected") ?: true,
            detectionType = jsonCondition.getInt("detectionType")
                ?.coerceIn(DETECTION_TYPE_LOWER_BOUND, DETECTION_TYPE_UPPER_BOUND)
                ?: DETECTION_TYPE_DEFAULT_VALUE,
            threshold = jsonCondition.getInt("threshold")
                ?.coerceIn(CONDITION_THRESHOLD_LOWER_BOUND, CONDITION_THRESHOLD_UPPER_BOUND)
                ?: CONDITION_THRESHOLD_DEFAULT_VALUE,
            detectionAreaLeft = jsonCondition.getInt("detectionAreaLeft"),
            detectionAreaTop = jsonCondition.getInt("detectionAreaTop"),
            detectionAreaRight = jsonCondition.getInt("detectionAreaRight"),
            detectionAreaBottom = jsonCondition.getInt("detectionAreaBottom"),
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeConditionTextDetected(jsonCondition: JsonObject): ConditionEntity? {
        val id = jsonCondition.getLong("id", true) ?: return null
        val eventId = jsonCondition.getLong("eventId", true) ?: return null
        val textToDetect = jsonCondition.getString("textToDetect", true) ?: return null
        val textLanguage = jsonCondition.getString("textLanguage", true) ?: return null

        return ConditionEntity(
            id = id,
            eventId = eventId,
            name = jsonCondition.getString("name") ?: "",
            priority = jsonCondition.getInt("priority") ?: 0,
            type = ConditionType.ON_IMAGE_DETECTED,
            textToDetect = textToDetect,
            textLanguage = textLanguage,
            shouldBeDetected = jsonCondition.getBoolean("shouldBeDetected") ?: true,
            detectionType = jsonCondition.getInt("detectionType")
                ?.coerceIn(DETECTION_TYPE_LOWER_BOUND, DETECTION_TYPE_UPPER_BOUND)
                ?: DETECTION_TYPE_DEFAULT_VALUE,
            threshold = jsonCondition.getInt("threshold")
                ?.coerceIn(CONDITION_THRESHOLD_LOWER_BOUND, CONDITION_THRESHOLD_UPPER_BOUND)
                ?: CONDITION_THRESHOLD_DEFAULT_VALUE,
            detectionAreaLeft = jsonCondition.getInt("detectionAreaLeft"),
            detectionAreaTop = jsonCondition.getInt("detectionAreaTop"),
            detectionAreaRight = jsonCondition.getInt("detectionAreaRight"),
            detectionAreaBottom = jsonCondition.getInt("detectionAreaBottom"),
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeConditionBroadcastReceived(jsonCondition: JsonObject): ConditionEntity? {
        val id = jsonCondition.getLong("id", true) ?: return null
        val eventId = jsonCondition.getLong("eventId", true) ?: return null
        val broadcastAction = jsonCondition.getString("broadcastAction") ?: return null

        return ConditionEntity(
            id = id,
            eventId = eventId,
            priority = 0,
            name = jsonCondition.getString("name") ?: "",
            type = ConditionType.ON_BROADCAST_RECEIVED,
            broadcastAction = broadcastAction,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeConditionCounterReached(jsonCondition: JsonObject): ConditionEntity? {
        val id = jsonCondition.getLong("id", true) ?: return null
        val eventId = jsonCondition.getLong("eventId", true) ?: return null
        val counterName = jsonCondition.getString("counterName") ?: return null
        val counterComparisonOperation = jsonCondition.getEnum<CounterComparisonOperation>("counterComparisonOperation")
            ?: return null

        val counterOperationValueType = jsonCondition.getEnum<CounterOperationValueType>("counterOperationValueType")
            ?: CounterOperationValueType.NUMBER
        val counterOperationValue = jsonCondition.getInt("counterValue") ?: 0
        val counterOperationCounterName = jsonCondition.getString("counterOperationCounterName") ?: ""

        return ConditionEntity(
            id = id,
            eventId = eventId,
            name = jsonCondition.getString("name") ?: "",
            priority = 0,
            type = ConditionType.ON_COUNTER_REACHED,
            counterName = counterName,
            counterComparisonOperation = counterComparisonOperation,
            counterOperationValueType = counterOperationValueType,
            counterValue = counterOperationValue,
            counterOperationCounterName = counterOperationCounterName,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeConditionTimerReached(jsonCondition: JsonObject): ConditionEntity? {
        val id = jsonCondition.getLong("id", true) ?: return null
        val eventId = jsonCondition.getLong("eventId", true) ?: return null
        val timerValueMs = jsonCondition.getLong("timerValueMs") ?: return null
        val restartWhenReached = jsonCondition.getBoolean("restartWhenReached") ?: return null

        return ConditionEntity(
            id = id,
            eventId = eventId,
            name = jsonCondition.getString("name") ?: "",
            priority = 0,
            type = ConditionType.ON_TIMER_REACHED,
            timerValueMs = timerValueMs,
            restartWhenReached = restartWhenReached,
        )
    }

    /** @return the type of condition. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeConditionType(jsonCondition: JsonObject): ConditionType? =
        jsonCondition.getEnum<ConditionType>("type", shouldLogError = true)


    // ======================= ACTION

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeAction(
        jsonAction: JsonObject,
        eventConditions: List<ConditionEntity>,
        conditionsOperator: Int,
    ): ActionEntity? =
        when (deserializeActionType(jsonAction)) {
            ActionType.CLICK -> deserializeActionClick(jsonAction, eventConditions, conditionsOperator)
            ActionType.SWIPE -> deserializeActionSwipe(jsonAction)
            ActionType.PAUSE -> deserializeActionPause(jsonAction)
            ActionType.INTENT -> deserializeActionIntent(jsonAction)
            ActionType.TOGGLE_EVENT -> deserializeActionToggleEvent(jsonAction)
            ActionType.CHANGE_COUNTER -> deserializeActionChangeCounter(jsonAction)
            ActionType.NOTIFICATION -> deserializeActionNotification(jsonAction)
            null -> null
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionClick(
        jsonClick: JsonObject,
        eventConditions: List<ConditionEntity>,
        conditionsOperator: Int,
    ): ActionEntity? {
        val id = jsonClick.getLong("id", true) ?: return null
        val eventId = jsonClick.getLong("eventId", true) ?: return null

        val x: Int?
        val y: Int?
        val clickOnConditionId: Long?
        val clickPositionType = jsonClick.getEnum<ClickPositionType>("clickPositionType", true)
            ?: return null
        val clickOffsetX: Int?
        val clickOffsetY: Int?

        val isAndConditionsOperator = conditionsOperator == 1

        when (clickPositionType) {
            ClickPositionType.ON_DETECTED_CONDITION -> {
                x = null
                y = null
                clickOnConditionId = jsonClick.getLong("clickOnConditionId", isAndConditionsOperator)
                if (isAndConditionsOperator && (clickOnConditionId == null || !eventConditions.containsId(clickOnConditionId))) {
                    Log.w(TAG, "Can't deserialize action, clickOnConditionId is not valid.")
                    return null
                }
                clickOffsetX = jsonClick.getInt("clickOffsetX")
                clickOffsetY = jsonClick.getInt("clickOffsetY")
            }

            ClickPositionType.USER_SELECTED -> {
                x = jsonClick.getInt("x", true) ?: return null
                y = jsonClick.getInt("y", true) ?: return null
                clickOnConditionId = null
                clickOffsetX = null
                clickOffsetY = null
            }
        }

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonClick.getString("name") ?: "",
            priority = jsonClick.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.CLICK,
            clickPositionType = clickPositionType,
            clickOnConditionId = clickOnConditionId,
            x = x,
            y = y,
            pressDuration = jsonClick.getLong("pressDuration")
                ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
                ?: DEFAULT_CLICK_DURATION,
            clickOffsetX = clickOffsetX,
            clickOffsetY = clickOffsetY,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionSwipe(jsonSwipe: JsonObject): ActionEntity? {
        val id = jsonSwipe.getLong("id", true) ?: return null
        val eventId = jsonSwipe.getLong("eventId", true) ?: return null
        val fromX = jsonSwipe.getInt("fromX", true) ?: return null
        val fromY = jsonSwipe.getInt("fromY", true) ?: return null
        val toX = jsonSwipe.getInt("toX", true) ?: return null
        val toY = jsonSwipe.getInt("toY", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonSwipe.getString("name") ?: "",
            priority = jsonSwipe.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.SWIPE,
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            swipeDuration = jsonSwipe.getLong("swipeDuration")
                ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
                ?: DEFAULT_SWIPE_DURATION,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionPause(jsonPause: JsonObject): ActionEntity? {
        val id = jsonPause.getLong("id", true) ?: return null
        val eventId = jsonPause.getLong("eventId", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonPause.getString("name") ?: "",
            priority = jsonPause.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.PAUSE,
            pauseDuration = jsonPause.getLong("pauseDuration")?.coerceAtLeast(0) ?: DEFAULT_PAUSE_DURATION,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionIntent(jsonIntent: JsonObject): ActionEntity? {
        val id = jsonIntent.getLong("id", true) ?: return null
        val eventId = jsonIntent.getLong("eventId", true) ?: return null
        val intentAction = jsonIntent.getString("intentAction", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonIntent.getString("name") ?: "",
            priority = jsonIntent.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.INTENT,
            isAdvanced = jsonIntent.getBoolean("isAdvanced") ?: false,
            isBroadcast = jsonIntent.getBoolean("isBroadcast") ?: false,
            intentAction = intentAction,
            componentName = jsonIntent.getString("componentName"),
            flags = jsonIntent.getInt("flags")?.coerceAtLeast(0) ?: 0,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionToggleEvent(jsonToggleEvent: JsonObject): ActionEntity? {
        val id = jsonToggleEvent.getLong("id", true) ?: return null
        val eventId = jsonToggleEvent.getLong("eventId", true) ?: return null
        val toggleAll = jsonToggleEvent.getBoolean("toggleAll") ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonToggleEvent.getString("name") ?: "",
            priority = jsonToggleEvent.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.TOGGLE_EVENT,
            toggleAll = toggleAll,
            toggleAllType = jsonToggleEvent.getEnum<EventToggleType>("toggleAllType"),
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionChangeCounter(jsonChangeCounter: JsonObject): ActionEntity? {
        val id = jsonChangeCounter.getLong("id", true) ?: return null
        val eventId = jsonChangeCounter.getLong("eventId", true) ?: return null
        val counterName = jsonChangeCounter.getString("counterName") ?: return null
        val counterOperation = jsonChangeCounter.getEnum<ChangeCounterOperationType>("counterOperation") ?: return null

        val counterOperationValueType = jsonChangeCounter.getEnum<CounterOperationValueType>("counterOperationValueType")
            ?: CounterOperationValueType.NUMBER
        val counterOperationValue = jsonChangeCounter.getInt("counterOperationValue") ?: 0
        val counterOperationCounterName = jsonChangeCounter.getString("counterOperationCounterName") ?: ""

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonChangeCounter.getString("name") ?: "",
            priority = jsonChangeCounter.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.CHANGE_COUNTER,
            counterName = counterName,
            counterOperation = counterOperation,
            counterOperationValueType = counterOperationValueType,
            counterOperationValue = counterOperationValue,
            counterOperationCounterName = counterOperationCounterName,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionNotification(jsonNotification: JsonObject): ActionEntity? {
        val id = jsonNotification.getLong("id", true) ?: return null
        val eventId = jsonNotification.getLong("eventId", true) ?: return null
        val channelImportance = jsonNotification.getInt("notification_importance") ?: return null
        val notificationMessageType = jsonNotification
            .getEnum<NotificationMessageType>("notification_message_type") ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonNotification.getString("name") ?: "",
            priority = jsonNotification.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.NOTIFICATION,
            notificationImportance = channelImportance,
            notificationMessageType = notificationMessageType,
            notificationMessageText = jsonNotification.getString("notification_message_text") ?: "",
            notificationMessageCounterName = jsonNotification.getString("notification_message_counter_name") ?: "",
        )
    }

    /** @return the type of condition. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeActionType(jsonAction: JsonObject): ActionType? =
        jsonAction.getEnum<ActionType>("type", shouldLogError = true)


    // ======================= INTENT EXTRA

    /** @return the deserialized intent extra. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeIntentExtra(jsonExtra: JsonObject): IntentExtraEntity? {
        val id = jsonExtra.getLong("id", true) ?: return null
        val actionId = jsonExtra.getLong("actionId", true) ?: return null
        val type = jsonExtra.getEnum<IntentExtraType>("type", true) ?: return null
        val key = jsonExtra.getString("key", true) ?: return null
        val value = jsonExtra.getString("value", true) ?: return null

        return IntentExtraEntity(id, actionId, type, key, value)
    }


    // ======================= EVENT TOGGLE

    /** @return the deserialized intent extra. */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun deserializeEventToggle(jsonExtra: JsonObject, scenarioEvents: List<EventEntity>): EventToggleEntity? {
        val id = jsonExtra.getLong("id", true) ?: return null
        val actionId = jsonExtra.getLong("actionId", true) ?: return null
        val type = jsonExtra.getEnum<EventToggleType>("type", true) ?: return null
        val toggleEventId = jsonExtra.getLong("toggleEventId", true) ?: return null

        if (!scenarioEvents.containsId(toggleEventId)) {
            Log.w(TAG, "Can't deserialize event toggle, toggleEventId is not valid.")
            return null
        }

        return EventToggleEntity(id, actionId, type, toggleEventId)
    }
}