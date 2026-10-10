/*
 * Copyright (C) 2026 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.base.extensions.getEnum
import com.buzbuz.smartautoclicker.core.base.extensions.getInt
import com.buzbuz.smartautoclicker.core.base.extensions.getLong
import com.buzbuz.smartautoclicker.core.base.extensions.getString
import com.buzbuz.smartautoclicker.core.base.interfaces.containsId
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerWithMotions

import kotlinx.serialization.json.JsonObject


/**
 * Deserializer for all JSON object version below 24.
 *
 * Before 24, the clicks and the swipes were declaring their own columns in the action table. They are now both
 * defined by a list of gesture fingers: a swipe becomes a gesture with one finger doing a press, a move and a
 * release, while a click keeps its [ActionType.CLICK] type but stores its position and press duration in one finger
 * doing a press, a wait and a release.
 */
internal open class CompatV24Deserializer : CompatDeserializer() {

    private companion object {
        const val LEGACY_ACTION_TYPE_SWIPE = "SWIPE"
        /** Default swipe duration in ms on compat deserialization. */
        const val DEFAULT_SWIPE_DURATION = 250L

        /** Tag for logs */
        const val TAG = "CompatV24Deserializer"
    }

    /** The fingers created for the action being deserialized. Reset for each of them. */
    private val migratedGestureFingers: MutableList<GestureFingerWithMotions> = mutableListOf()
    private var currentTemporaryId: Long = -1

    override fun deserializeCompleteAction(
        jsonCompleteAction: JsonObject,
        scenarioEvents: List<EventEntity>,
        eventConditions: List<ConditionEntity>,
        conditionsOperator: Int
    ): CompleteActionEntity? {
        migratedGestureFingers.clear()

        val completeAction =
            super.deserializeCompleteAction(jsonCompleteAction, scenarioEvents, eventConditions, conditionsOperator)
                ?: return null

        return if (migratedGestureFingers.isEmpty()) completeAction
        else completeAction.copy(gestureFingers = migratedGestureFingers.toList())
    }

    override fun deserializeAction(
        jsonAction: JsonObject,
        eventConditions: List<ConditionEntity>,
        conditionsOperator: Int,
    ): ActionEntity? =
        if (jsonAction.getString("type") == LEGACY_ACTION_TYPE_SWIPE) deserializeLegacySwipe(jsonAction)
        else super.deserializeAction(jsonAction, eventConditions, conditionsOperator)

    override fun deserializeActionClick(
        jsonClick: JsonObject,
        eventConditions: List<ConditionEntity>,
        conditionsOperator: Int,
    ): ActionEntity? {
        val clickPositionType = jsonClick.getEnum<ClickPositionType>("clickPositionType", true)
            ?: return null

        return when (clickPositionType) {
            ClickPositionType.ON_DETECTED_CONDITION -> {
                val isAndConditionsOperator = conditionsOperator == 1
                val clickOnConditionId = jsonClick.getLong("clickOnConditionId", isAndConditionsOperator)
                if (isAndConditionsOperator && (clickOnConditionId == null || !eventConditions.containsId(clickOnConditionId))) {
                    Log.w(TAG, "Can't deserialize action, clickOnConditionId is not valid.")
                    return null
                }

                deserializeLegacyClick(
                    jsonClick = jsonClick,
                    positionType = clickPositionType,
                    clickOnConditionId = clickOnConditionId,
                    clickOffsetX = jsonClick.getInt("clickOffsetX"),
                    clickOffsetY = jsonClick.getInt("clickOffsetY"),
                )
            }

            ClickPositionType.USER_SELECTED ->
                deserializeLegacyClick(
                    jsonClick = jsonClick,
                    positionType = clickPositionType,
                    x = jsonClick.getInt("x", true) ?: return null,
                    y = jsonClick.getInt("y", true) ?: return null,
                )
        }
    }

    protected fun deserializeLegacyClick(
        jsonClick: JsonObject,
        positionType: ClickPositionType,
        x: Int? = null,
        y: Int? = null,
        clickOnConditionId: Long? = null,
        clickOffsetX: Int? = null,
        clickOffsetY: Int? = null,
    ): ActionEntity? {
        val id = jsonClick.getLong("id", true) ?: return null
        val eventId = jsonClick.getLong("eventId", true) ?: return null
        val pressDurationMs = jsonClick.getLong("pressDuration")
            ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
            ?: DEFAULT_CLICK_DURATION

        val isOnCondition = positionType == ClickPositionType.ON_DETECTED_CONDITION
        addMigratedGestureFinger(
            actionId = id,
            motions = { fingerId ->
                listOf(
                    GestureFingerMotionEntity(
                        id = getNextId(),
                        fingerId = fingerId,
                        index = 0,
                        motionType = GestureFingerMotionType.PRESS,
                        xCoordinateType = if (isOnCondition) CoordinatesType.CONDITION else CoordinatesType.LITERAL,
                        yCoordinateType = if (isOnCondition) CoordinatesType.CONDITION else CoordinatesType.LITERAL,
                        conditionId = clickOnConditionId,
                        xLiteral = x,
                        yLiteral = y,
                        xOffsetCoordinateType = clickOffsetX?.let { CoordinatesType.LITERAL },
                        yOffsetCoordinateType = clickOffsetY?.let { CoordinatesType.LITERAL },
                        xOffsetLiteral = clickOffsetX,
                        yOffsetLiteral = clickOffsetY,
                    ),
                    GestureFingerMotionEntity(
                        id = getNextId(),
                        fingerId = fingerId,
                        index = 1,
                        motionType = GestureFingerMotionType.WAIT,
                        durationMs = pressDurationMs,
                    ),
                    GestureFingerMotionEntity(
                        id = getNextId(),
                        fingerId = fingerId,
                        index = 2,
                        motionType = GestureFingerMotionType.RELEASE,
                    ),
                )
            },
        )

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonClick.getString("name") ?: "",
            priority = jsonClick.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.CLICK,
        )
    }

    private fun deserializeLegacySwipe(jsonSwipe: JsonObject): ActionEntity? {
        val id = jsonSwipe.getLong("id", true) ?: return null
        val eventId = jsonSwipe.getLong("eventId", true) ?: return null
        val fromX = jsonSwipe.getInt("fromX", true) ?: return null
        val fromY = jsonSwipe.getInt("fromY", true) ?: return null
        val toX = jsonSwipe.getInt("toX", true) ?: return null
        val toY = jsonSwipe.getInt("toY", true) ?: return null
        val swipeDurationMs = jsonSwipe.getLong("swipeDuration")
            ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
            ?: DEFAULT_SWIPE_DURATION

        addMigratedGestureFinger(
            actionId = id,
            motions = { fingerId ->
                listOf(
                    GestureFingerMotionEntity(
                        id = getNextId(),
                        fingerId = fingerId,
                        index = 0,
                        motionType = GestureFingerMotionType.PRESS,
                        xCoordinateType = CoordinatesType.LITERAL,
                        yCoordinateType = CoordinatesType.LITERAL,
                        xLiteral = fromX,
                        yLiteral = fromY,
                    ),
                    GestureFingerMotionEntity(
                        id = getNextId(),
                        fingerId = fingerId,
                        index = 1,
                        motionType = GestureFingerMotionType.MOVE,
                        xCoordinateType = CoordinatesType.LITERAL,
                        yCoordinateType = CoordinatesType.LITERAL,
                        xLiteral = toX,
                        yLiteral = toY,
                        durationMs = swipeDurationMs,
                    ),
                    GestureFingerMotionEntity(
                        id = getNextId(),
                        fingerId = fingerId,
                        index = 2,
                        motionType = GestureFingerMotionType.RELEASE,
                    ),
                )
            },
        )

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonSwipe.getString("name") ?: "",
            priority = jsonSwipe.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.GESTURE,
        )
    }

    /** Clicks and swipes were supporting only one finger, so there is never more than one to create. */
    private fun addMigratedGestureFinger(
        actionId: Long,
        motions: (fingerId: Long) -> List<GestureFingerMotionEntity>,
    ) {
        val fingerId = getNextId()
        migratedGestureFingers.add(
            GestureFingerWithMotions(
                finger = GestureFingerEntity(
                    id = fingerId,
                    actionId = actionId,
                    index = 0,
                    startTimeMs = 0,
                ),
                motions = motions(fingerId),
            )
        )
    }

    private fun getNextId(): Long = --currentTemporaryId
}
