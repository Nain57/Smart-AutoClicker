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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.EventType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.serialization.DeserializerFactory
import com.buzbuz.smartautoclicker.core.database.utils.toJsonArray

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Test the [CompatV24Deserializer] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CompatV24DeserializerTests {

    private companion object {
        private const val VERSION_MINIMUM = 20
        private const val VERSION_MAXIMUM = 23
        private const val SCENARIO_ID = 42L
        private const val EVENT_ID = 1L
        private const val CONDITION_ID = 1L
    }

    private val deserializer = CompatV24Deserializer()

    // ======================= JSON builder helpers =======================

    private fun createJsonCompleteScenario(
        scenarioJson: JsonObject = createJsonScenario(),
        completeEventsJson: List<JsonObject> = emptyList(),
    ): JsonObject = JsonObject(
        mapOf(
            "scenario" to scenarioJson,
            "events" to completeEventsJson.toJsonArray(),
        )
    )

    private fun createJsonScenario(): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(SCENARIO_ID),
            "name" to JsonPrimitive("test"),
            "detectionQuality" to JsonPrimitive(1000),
        )
    )

    private fun createJsonCompleteEvent(
        eventJson: JsonObject = createJsonEvent(),
        conditionsJson: List<JsonObject>,
        actionsJson: List<JsonObject>,
    ): JsonObject = JsonObject(
        mapOf(
            "event" to eventJson,
            "conditions" to conditionsJson.toJsonArray(),
            "actions" to actionsJson.toJsonArray(),
        )
    )

    private fun createJsonEvent(id: Long = EVENT_ID): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "scenarioId" to JsonPrimitive(SCENARIO_ID),
            "name" to JsonPrimitive("event"),
            "type" to JsonPrimitive(EventType.IMAGE_EVENT.name),
            "conditionOperator" to JsonPrimitive(1),
            "priority" to JsonPrimitive(0),
            "enabledOnStart" to JsonPrimitive(true),
        )
    )

    private fun createJsonCompleteAction(actionJson: JsonObject): JsonObject = JsonObject(
        mapOf(
            "action" to actionJson,
            "intentExtras" to emptyList<JsonObject>().toJsonArray(),
        )
    )

    private fun createJsonSwipeAction(
        id: Long = 1L,
        eventId: Long = EVENT_ID,
        name: String = "swipe",
        fromX: Int? = 10,
        fromY: Int? = 20,
        toX: Int? = 30,
        toY: Int? = 40,
        swipeDuration: Long? = 500L,
    ): JsonObject = JsonObject(
        buildMap {
            put("id", JsonPrimitive(id))
            put("eventId", JsonPrimitive(eventId))
            put("name", JsonPrimitive(name))
            put("priority", JsonPrimitive(0))
            put("type", JsonPrimitive("SWIPE"))
            if (fromX != null) put("fromX", JsonPrimitive(fromX))
            if (fromY != null) put("fromY", JsonPrimitive(fromY))
            if (toX != null) put("toX", JsonPrimitive(toX))
            if (toY != null) put("toY", JsonPrimitive(toY))
            if (swipeDuration != null) put("swipeDuration", JsonPrimitive(swipeDuration))
        }
    )

    private fun createJsonClickAction(
        id: Long = 1L,
        eventId: Long = EVENT_ID,
        name: String = "click",
        positionType: String = ClickPositionType.USER_SELECTED.name,
        x: Int? = 10,
        y: Int? = 20,
        clickOnConditionId: Long? = null,
        clickOffsetX: Int? = null,
        clickOffsetY: Int? = null,
        pressDuration: Long? = 250L,
    ): JsonObject = JsonObject(
        buildMap {
            put("id", JsonPrimitive(id))
            put("eventId", JsonPrimitive(eventId))
            put("name", JsonPrimitive(name))
            put("priority", JsonPrimitive(0))
            put("type", JsonPrimitive(ActionType.CLICK.name))
            put("clickPositionType", JsonPrimitive(positionType))
            if (x != null) put("x", JsonPrimitive(x))
            if (y != null) put("y", JsonPrimitive(y))
            if (clickOnConditionId != null) put("clickOnConditionId", JsonPrimitive(clickOnConditionId))
            if (clickOffsetX != null) put("clickOffsetX", JsonPrimitive(clickOffsetX))
            if (clickOffsetY != null) put("clickOffsetY", JsonPrimitive(clickOffsetY))
            if (pressDuration != null) put("pressDuration", JsonPrimitive(pressDuration))
        }
    )

    private fun createJsonPauseAction(id: Long = 99L, eventId: Long = EVENT_ID): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "eventId" to JsonPrimitive(eventId),
            "name" to JsonPrimitive("pause"),
            "priority" to JsonPrimitive(0),
            "type" to JsonPrimitive(ActionType.PAUSE.name),
            "pauseDuration" to JsonPrimitive(50L),
        )
    )

    private fun createJsonConditionImage(id: Long = 1L, eventId: Long = EVENT_ID): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "eventId" to JsonPrimitive(eventId),
            "name" to JsonPrimitive("img_cond"),
            "type" to JsonPrimitive("ON_IMAGE_DETECTED"),
            "priority" to JsonPrimitive(0),
            "path" to JsonPrimitive("/test/image"),
            "areaLeft" to JsonPrimitive(0),
            "areaTop" to JsonPrimitive(0),
            "areaRight" to JsonPrimitive(100),
            "areaBottom" to JsonPrimitive(100),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(4),
        )
    )

    // ======================= Factory =======================

    @Test
    fun deserialization_factory_availability() {
        for (i in VERSION_MINIMUM..VERSION_MAXIMUM) {
            DeserializerFactory.create(i).let { d ->
                assertNotNull(d)
                assertTrue(d is CompatV24Deserializer)
            }
        }
    }

    // ======================= Required field null-safety =======================

    @Test
    fun deserializeLegacySwipe_missingId_actionNotDeserialized() {
        val swipeJson = JsonObject(buildMap {
            // no "id"
            put("eventId", JsonPrimitive(EVENT_ID))
            put("type", JsonPrimitive("SWIPE"))
            put("fromX", JsonPrimitive(10))
            put("fromY", JsonPrimitive(20))
            put("toX", JsonPrimitive(30))
            put("toY", JsonPrimitive(40))
            put("swipeDuration", JsonPrimitive(500L))
        })
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(swipeJson)),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacySwipe_missingEventId_actionNotDeserialized() {
        val swipeJson = JsonObject(buildMap {
            put("id", JsonPrimitive(1L))
            // no "eventId"
            put("type", JsonPrimitive("SWIPE"))
            put("fromX", JsonPrimitive(10))
            put("fromY", JsonPrimitive(20))
            put("toX", JsonPrimitive(30))
            put("toY", JsonPrimitive(40))
            put("swipeDuration", JsonPrimitive(500L))
        })
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(swipeJson)),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacySwipe_missingFromX_actionNotDeserialized() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(fromX = null))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacySwipe_missingFromY_actionNotDeserialized() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(fromY = null))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacySwipe_missingToX_actionNotDeserialized() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(toX = null))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacySwipe_missingToY_actionNotDeserialized() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(toY = null))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    // ======================= Action entity type =======================

    @Test
    fun deserializeLegacySwipe_actionTypeIsGesture() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction())),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertEquals(ActionType.GESTURE, result.events.first().actions.first().action.type)
    }

    @Test
    fun nonSwipe_actionType_passedThrough() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonPauseAction())),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertEquals(ActionType.PAUSE, result.events.first().actions.first().action.type)
    }

    // ======================= Duration handling =======================

    @Test
    fun deserializeLegacySwipe_missingDuration_usesDefault250ms() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(swipeDuration = null))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val moveMotion = result.events.first().actions.first().gestureFingers.first()
            .motions.first { it.motionType == GestureFingerMotionType.MOVE }
        assertEquals(250L, moveMotion.durationMs)
    }

    @Test
    fun deserializeLegacySwipe_durationBelowLowerBound_clampedToOne() {
        // DURATION_LOWER_BOUND = 1L; 0 is below it
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(swipeDuration = 0L))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val moveMotion = result.events.first().actions.first().gestureFingers.first()
            .motions.first { it.motionType == GestureFingerMotionType.MOVE }
        assertEquals(1L, moveMotion.durationMs)
    }

    @Test
    fun deserializeLegacySwipe_durationAboveUpperBound_clampedToMax() {
        // DURATION_GESTURE_UPPER_BOUND = 59_999L; 100_000 is above it
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(swipeDuration = 100_000L))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val moveMotion = result.events.first().actions.first().gestureFingers.first()
            .motions.first { it.motionType == GestureFingerMotionType.MOVE }
        assertEquals(59_999L, moveMotion.durationMs)
    }

    // ======================= Gesture finger structure =======================

    @Test
    fun deserializeLegacySwipe_oneFinger() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction())),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertEquals(1, result.events.first().actions.first().gestureFingers.size)
    }

    @Test
    fun deserializeLegacySwipe_finger_hasCorrectStructure() {
        val actionId = 7L
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(id = actionId))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val finger = result.events.first().actions.first().gestureFingers.first().finger
        assertEquals(actionId, finger.actionId)
        assertEquals(0, finger.index)
        assertEquals(0L, finger.startTimeMs)
        assertTrue("finger.id should be a negative temporary id", finger.id < 0)
    }

    // ======================= Gesture motion structure =======================

    @Test
    fun deserializeLegacySwipe_threeMotions() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction())),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertEquals(3, result.events.first().actions.first().gestureFingers.first().motions.size)
    }

    @Test
    fun deserializeLegacySwipe_pressMotion_hasFromCoordinates() {
        val fromX = 10
        val fromY = 20
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction(fromX = fromX, fromY = fromY))),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val pressMotion = result.events.first().actions.first().gestureFingers.first().motions[0]
        assertEquals(GestureFingerMotionType.PRESS, pressMotion.motionType)
        assertEquals(0, pressMotion.index)
        assertEquals(CoordinatesType.LITERAL, pressMotion.xCoordinateType)
        assertEquals(CoordinatesType.LITERAL, pressMotion.yCoordinateType)
        assertEquals(fromX, pressMotion.xLiteral)
        assertEquals(fromY, pressMotion.yLiteral)
        assertNull(pressMotion.durationMs)
    }

    @Test
    fun deserializeLegacySwipe_moveMotion_hasToCoordinatesAndDuration() {
        val toX = 30
        val toY = 40
        val duration = 500L
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonSwipeAction(toX = toX, toY = toY, swipeDuration = duration))
                    ),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val moveMotion = result.events.first().actions.first().gestureFingers.first().motions[1]
        assertEquals(GestureFingerMotionType.MOVE, moveMotion.motionType)
        assertEquals(1, moveMotion.index)
        assertEquals(CoordinatesType.LITERAL, moveMotion.xCoordinateType)
        assertEquals(CoordinatesType.LITERAL, moveMotion.yCoordinateType)
        assertEquals(toX, moveMotion.xLiteral)
        assertEquals(toY, moveMotion.yLiteral)
        assertEquals(duration, moveMotion.durationMs)
    }

    @Test
    fun deserializeLegacySwipe_releaseMotion_hasNoCoordinates() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction())),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val releaseMotion = result.events.first().actions.first().gestureFingers.first().motions[2]
        assertEquals(GestureFingerMotionType.RELEASE, releaseMotion.motionType)
        assertEquals(2, releaseMotion.index)
        assertNull(releaseMotion.xCoordinateType)
        assertNull(releaseMotion.yCoordinateType)
        assertNull(releaseMotion.xLiteral)
        assertNull(releaseMotion.yLiteral)
        assertNull(releaseMotion.durationMs)
    }

    // ======================= Temporary IDs =======================

    @Test
    fun deserializeLegacySwipe_temporaryIds_areNegative() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonSwipeAction())),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val fingerWithMotions = result.events.first().actions.first().gestureFingers.first()
        assertTrue("finger.id should be negative", fingerWithMotions.finger.id < 0)
        fingerWithMotions.motions.forEach { motion ->
            assertTrue("motion.id should be negative", motion.id < 0)
        }
    }

    // ======================= Non-swipe passes through =======================

    @Test
    fun nonSwipe_gestureFingers_isEmpty() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(createJsonCompleteAction(createJsonPauseAction())),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.first().actions.first().gestureFingers.isEmpty())
    }

    // ======================= Multiple swipes =======================

    @Test
    fun deserializeLegacySwipe_multipleSwipes_allActionsAreGestures() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonSwipeAction(id = 1L)),
                        createJsonCompleteAction(createJsonSwipeAction(id = 2L)),
                    ),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val actions = result.events.first().actions
        assertEquals(2, actions.size)
        assertEquals(ActionType.GESTURE, actions[0].action.type)
        assertEquals(ActionType.GESTURE, actions[1].action.type)
    }

    @Test
    fun deserializeLegacySwipe_multipleSwipes_eachHasGestureFingers() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonSwipeAction(id = 1L)),
                        createJsonCompleteAction(createJsonSwipeAction(id = 2L)),
                    ),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val actions = result.events.first().actions
        assertTrue(actions[0].gestureFingers.isNotEmpty())
        assertTrue(actions[1].gestureFingers.isNotEmpty())
    }

    // ======================= Legacy clicks =======================

    @Test
    fun deserializeLegacyClick_actionTypeIsStillClick() {
        val result = deserializer.deserializeCompleteScenario(createScenarioWithClick(createJsonClickAction()))

        assertEquals(ActionType.CLICK, result.events.first().actions.first().action.type)
    }

    @Test
    fun deserializeLegacyClick_missingPositionType_actionNotDeserialized() {
        val clickJson = JsonObject(
            mapOf(
                "id" to JsonPrimitive(1L),
                "eventId" to JsonPrimitive(EVENT_ID),
                "name" to JsonPrimitive("click"),
                "priority" to JsonPrimitive(0),
                "type" to JsonPrimitive(ActionType.CLICK.name),
                "x" to JsonPrimitive(10),
                "y" to JsonPrimitive(20),
            )
        )

        val result = deserializer.deserializeCompleteScenario(createScenarioWithClick(clickJson))

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacyClick_userSelected_threeMotions() {
        val result = deserializer.deserializeCompleteScenario(createScenarioWithClick(createJsonClickAction()))

        val motions = result.events.first().actions.first().gestureFingers.first().motions
        assertEquals(3, motions.size)
        assertEquals(GestureFingerMotionType.PRESS, motions[0].motionType)
        assertEquals(GestureFingerMotionType.WAIT, motions[1].motionType)
        assertEquals(GestureFingerMotionType.RELEASE, motions[2].motionType)
    }

    @Test
    fun deserializeLegacyClick_userSelected_pressMotionHasCoordinates() {
        val x = 42
        val y = 84
        val result = deserializer
            .deserializeCompleteScenario(createScenarioWithClick(createJsonClickAction(x = x, y = y)))

        val pressMotion = result.events.first().actions.first().gestureFingers.first().motions[0]
        assertEquals(CoordinatesType.LITERAL, pressMotion.xCoordinateType)
        assertEquals(CoordinatesType.LITERAL, pressMotion.yCoordinateType)
        assertEquals(x, pressMotion.xLiteral)
        assertEquals(y, pressMotion.yLiteral)
        assertNull(pressMotion.conditionId)
    }

    @Test
    fun deserializeLegacyClick_userSelected_missingCoordinates_actionNotDeserialized() {
        val result = deserializer
            .deserializeCompleteScenario(createScenarioWithClick(createJsonClickAction(x = null)))

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacyClick_pressDurationIsWaitMotionDuration() {
        val pressDuration = 1234L
        val result = deserializer.deserializeCompleteScenario(
            createScenarioWithClick(createJsonClickAction(pressDuration = pressDuration))
        )

        val waitMotion = result.events.first().actions.first().gestureFingers.first().motions[1]
        assertEquals(pressDuration, waitMotion.durationMs)
    }

    @Test
    fun deserializeLegacyClick_missingPressDuration_usesDefault() {
        val result = deserializer.deserializeCompleteScenario(
            createScenarioWithClick(createJsonClickAction(pressDuration = null))
        )

        val waitMotion = result.events.first().actions.first().gestureFingers.first().motions[1]
        assertEquals(1L, waitMotion.durationMs)
    }

    @Test
    fun deserializeLegacyClick_onCondition_pressMotionHasConditionPosition() {
        val result = deserializer.deserializeCompleteScenario(
            createScenarioWithClick(
                createJsonClickAction(
                    positionType = ClickPositionType.ON_DETECTED_CONDITION.name,
                    x = null,
                    y = null,
                    clickOnConditionId = CONDITION_ID,
                )
            )
        )

        val pressMotion = result.events.first().actions.first().gestureFingers.first().motions[0]
        assertEquals(CoordinatesType.CONDITION, pressMotion.xCoordinateType)
        assertEquals(CoordinatesType.CONDITION, pressMotion.yCoordinateType)
        assertEquals(CONDITION_ID, pressMotion.conditionId)
        assertNull(pressMotion.xLiteral)
        assertNull(pressMotion.yLiteral)
    }

    @Test
    fun deserializeLegacyClick_onCondition_offsetIsInPressMotion() {
        val offsetX = -5
        val offsetY = 15
        val result = deserializer.deserializeCompleteScenario(
            createScenarioWithClick(
                createJsonClickAction(
                    positionType = ClickPositionType.ON_DETECTED_CONDITION.name,
                    x = null,
                    y = null,
                    clickOnConditionId = CONDITION_ID,
                    clickOffsetX = offsetX,
                    clickOffsetY = offsetY,
                )
            )
        )

        val pressMotion = result.events.first().actions.first().gestureFingers.first().motions[0]
        assertEquals(CoordinatesType.LITERAL, pressMotion.xOffsetCoordinateType)
        assertEquals(CoordinatesType.LITERAL, pressMotion.yOffsetCoordinateType)
        assertEquals(offsetX, pressMotion.xOffsetLiteral)
        assertEquals(offsetY, pressMotion.yOffsetLiteral)
    }

    @Test
    fun deserializeLegacyClick_onCondition_withoutOffset_pressMotionHasNoOffset() {
        val result = deserializer.deserializeCompleteScenario(
            createScenarioWithClick(
                createJsonClickAction(
                    positionType = ClickPositionType.ON_DETECTED_CONDITION.name,
                    x = null,
                    y = null,
                    clickOnConditionId = CONDITION_ID,
                )
            )
        )

        val pressMotion = result.events.first().actions.first().gestureFingers.first().motions[0]
        assertNull(pressMotion.xOffsetCoordinateType)
        assertNull(pressMotion.yOffsetCoordinateType)
        assertNull(pressMotion.xOffsetLiteral)
        assertNull(pressMotion.yOffsetLiteral)
    }

    @Test
    fun deserializeLegacyClick_onCondition_invalidConditionId_actionNotDeserialized() {
        val result = deserializer.deserializeCompleteScenario(
            createScenarioWithClick(
                createJsonClickAction(
                    positionType = ClickPositionType.ON_DETECTED_CONDITION.name,
                    x = null,
                    y = null,
                    clickOnConditionId = 4242L,
                )
            )
        )

        assertTrue(result.events.flatMap { it.actions }.isEmpty())
    }

    @Test
    fun deserializeLegacyClick_afterASwipe_hasItsOwnFingers() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonSwipeAction(id = 1L)),
                        createJsonCompleteAction(createJsonClickAction(id = 2L)),
                    ),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        val actions = result.events.first().actions
        assertEquals(2L, actions[1].gestureFingers.first().finger.actionId)
        assertEquals(GestureFingerMotionType.WAIT, actions[1].gestureFingers.first().motions[1].motionType)
    }

    @Test
    fun nonMigratedAction_afterAClick_hasNoFingers() {
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonClickAction(id = 1L)),
                        createJsonCompleteAction(createJsonPauseAction(id = 2L)),
                    ),
                )
            )
        )

        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        assertTrue(result.events.first().actions[1].gestureFingers.isEmpty())
    }

    private fun createScenarioWithClick(clickJson: JsonObject): JsonObject =
        createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage(id = CONDITION_ID)),
                    actionsJson = listOf(createJsonCompleteAction(clickJson)),
                )
            )
        )
}
