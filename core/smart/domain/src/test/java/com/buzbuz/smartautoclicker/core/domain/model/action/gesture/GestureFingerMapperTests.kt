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
package com.buzbuz.smartautoclicker.core.domain.model.action.gesture

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerWithMotions
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue.Counter
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue.Literal

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureFingerMapperTests {

    /* ======================== Test constants ======================== */

    private val fingerId = Identifier(databaseId = 100L)
    private val pressMotionId = Identifier(databaseId = 101L)
    private val moveMotionId = Identifier(databaseId = 102L)
    private val waitMotionId = Identifier(databaseId = 103L)
    private val releaseMotionId = Identifier(databaseId = 104L)
    private val conditionId = Identifier(databaseId = 200L)

    private val actionId = 8L
    private val fingerIndex = 0

    private val startTimeMs = 50L
    private val pressX = 42
    private val pressY = 78
    private val moveToX = 789
    private val moveToY = 456
    private val moveDurationMs = 500L
    private val waitDurationMs = 250L
    private val offsetX = 10
    private val offsetY = 20
    private val counterXName = "counterX"
    private val counterYName = "counterY"

    /* ======================== toFingerEntity ======================== */

    @Test
    fun fingerToEntity() {
        val finger = GestureFinger(
            id = fingerId,
            startTimeMs = startTimeMs,
            motions = listOf(
                GestureFingerMotion.Press(pressMotionId, GesturePosition.Coordinates(Literal(pressX), Literal(pressY)), GesturePosition.Coordinates(Literal(0), Literal(0))),
                GestureFingerMotion.Release(releaseMotionId),
            ),
        )

        assertEquals(
            GestureFingerEntity(id = fingerId.databaseId, actionId = actionId, index = fingerIndex, startTimeMs = startTimeMs),
            finger.toFingerEntity(actionId = actionId, index = fingerIndex),
        )
    }

    /* ======================== toMotionEntity — Press ======================== */

    @Test
    fun pressMotion_literalCoords_toEntity() {
        val press = GestureFingerMotion.Press(
            id = pressMotionId,
            position = GesturePosition.Coordinates(Literal(pressX), Literal(pressY)),
            offset = GesturePosition.Coordinates(Literal(offsetX), Literal(offsetY)),
        )

        assertEquals(
            GestureFingerMotionEntity(
                id = pressMotionId.databaseId,
                fingerId = fingerId.databaseId,
                index = 0,
                motionType = GestureFingerMotionType.PRESS,
                xCoordinateType = CoordinatesType.LITERAL,
                yCoordinateType = CoordinatesType.LITERAL,
                xLiteral = pressX,
                yLiteral = pressY,
                xOffsetCoordinateType = CoordinatesType.LITERAL,
                yOffsetCoordinateType = CoordinatesType.LITERAL,
                xOffsetLiteral = offsetX,
                yOffsetLiteral = offsetY,
            ),
            press.toMotionEntity(fingerId = fingerId.databaseId, index = 0),
        )
    }

    @Test
    fun pressMotion_counterCoords_toEntity() {
        val press = GestureFingerMotion.Press(
            id = pressMotionId,
            position = GesturePosition.Coordinates(Counter(counterXName), Counter(counterYName)),
            offset = GesturePosition.Coordinates(Literal(0), Literal(0)),
        )

        assertEquals(
            GestureFingerMotionEntity(
                id = pressMotionId.databaseId,
                fingerId = fingerId.databaseId,
                index = 0,
                motionType = GestureFingerMotionType.PRESS,
                xCoordinateType = CoordinatesType.COUNTER,
                yCoordinateType = CoordinatesType.COUNTER,
                xCounter = counterXName,
                yCounter = counterYName,
                xOffsetCoordinateType = CoordinatesType.LITERAL,
                yOffsetCoordinateType = CoordinatesType.LITERAL,
                xOffsetLiteral = 0,
                yOffsetLiteral = 0,
            ),
            press.toMotionEntity(fingerId = fingerId.databaseId, index = 0),
        )
    }

    @Test
    fun pressMotion_conditionPosition_toEntity() {
        val press = GestureFingerMotion.Press(
            id = pressMotionId,
            position = GesturePosition.Condition(conditionId = conditionId),
            offset = GesturePosition.Coordinates(Literal(0), Literal(0)),
        )

        assertEquals(
            GestureFingerMotionEntity(
                id = pressMotionId.databaseId,
                fingerId = fingerId.databaseId,
                index = 0,
                motionType = GestureFingerMotionType.PRESS,
                xCoordinateType = CoordinatesType.CONDITION,
                yCoordinateType = CoordinatesType.CONDITION,
                conditionId = conditionId.databaseId,
                xOffsetCoordinateType = CoordinatesType.LITERAL,
                yOffsetCoordinateType = CoordinatesType.LITERAL,
                xOffsetLiteral = 0,
                yOffsetLiteral = 0,
            ),
            press.toMotionEntity(fingerId = fingerId.databaseId, index = 0),
        )
    }

    /* ======================== toMotionEntity — Move ======================== */

    @Test
    fun moveMotion_literalCoords_toEntity() {
        val move = GestureFingerMotion.Move(
            id = moveMotionId,
            position = GesturePosition.Coordinates(Literal(moveToX), Literal(moveToY)),
            offset = GesturePosition.Coordinates(Literal(offsetX), Literal(offsetY)),
            durationMs = moveDurationMs,
        )

        assertEquals(
            GestureFingerMotionEntity(
                id = moveMotionId.databaseId,
                fingerId = fingerId.databaseId,
                index = 1,
                motionType = GestureFingerMotionType.MOVE,
                durationMs = moveDurationMs,
                xCoordinateType = CoordinatesType.LITERAL,
                yCoordinateType = CoordinatesType.LITERAL,
                xLiteral = moveToX,
                yLiteral = moveToY,
                xOffsetCoordinateType = CoordinatesType.LITERAL,
                yOffsetCoordinateType = CoordinatesType.LITERAL,
                xOffsetLiteral = offsetX,
                yOffsetLiteral = offsetY,
            ),
            move.toMotionEntity(fingerId = fingerId.databaseId, index = 1),
        )
    }

    /* ======================== toMotionEntity — Wait ======================== */

    @Test
    fun waitMotion_toEntity() {
        val wait = GestureFingerMotion.Wait(
            id = waitMotionId,
            durationMs = waitDurationMs,
        )

        assertEquals(
            GestureFingerMotionEntity(
                id = waitMotionId.databaseId,
                fingerId = fingerId.databaseId,
                index = 2,
                motionType = GestureFingerMotionType.WAIT,
                durationMs = waitDurationMs,
            ),
            wait.toMotionEntity(fingerId = fingerId.databaseId, index = 2),
        )
    }

    /* ======================== toMotionEntity — Release ======================== */

    @Test
    fun releaseMotion_toEntity() {
        val release = GestureFingerMotion.Release(id = releaseMotionId)

        assertEquals(
            GestureFingerMotionEntity(
                id = releaseMotionId.databaseId,
                fingerId = fingerId.databaseId,
                index = 3,
                motionType = GestureFingerMotionType.RELEASE,
            ),
            release.toMotionEntity(fingerId = fingerId.databaseId, index = 3),
        )
    }

    /* ======================== toDomainGestureFinger ======================== */

    @Test
    fun fingerWithMotions_pressRelease_toDomain() {
        val fingerWithMotions = GestureFingerWithMotions(
            finger = GestureFingerEntity(fingerId.databaseId, actionId, fingerIndex, startTimeMs),
            motions = listOf(
                GestureFingerMotionEntity(
                    id = pressMotionId.databaseId, fingerId = fingerId.databaseId, index = 0,
                    motionType = GestureFingerMotionType.PRESS,
                    xCoordinateType = CoordinatesType.LITERAL, yCoordinateType = CoordinatesType.LITERAL,
                    xLiteral = pressX, yLiteral = pressY,
                    xOffsetCoordinateType = CoordinatesType.LITERAL, yOffsetCoordinateType = CoordinatesType.LITERAL,
                    xOffsetLiteral = offsetX, yOffsetLiteral = offsetY,
                ),
                GestureFingerMotionEntity(
                    id = releaseMotionId.databaseId, fingerId = fingerId.databaseId, index = 1,
                    motionType = GestureFingerMotionType.RELEASE,
                ),
            ),
        )

        assertEquals(
            GestureFinger(
                id = fingerId,
                startTimeMs = startTimeMs,
                motions = listOf(
                    GestureFingerMotion.Press(
                        id = pressMotionId,
                        position = GesturePosition.Coordinates(Literal(pressX), Literal(pressY)),
                        offset = GesturePosition.Coordinates(Literal(offsetX), Literal(offsetY)),
                    ),
                    GestureFingerMotion.Release(id = releaseMotionId),
                ),
            ),
            fingerWithMotions.toDomainGestureFinger(),
        )
    }

    @Test
    fun fingerWithMotions_fullSequence_toDomain() {
        val fingerWithMotions = GestureFingerWithMotions(
            finger = GestureFingerEntity(fingerId.databaseId, actionId, fingerIndex, startTimeMs),
            motions = listOf(
                GestureFingerMotionEntity(
                    id = pressMotionId.databaseId, fingerId = fingerId.databaseId, index = 0,
                    motionType = GestureFingerMotionType.PRESS,
                    xCoordinateType = CoordinatesType.LITERAL, yCoordinateType = CoordinatesType.LITERAL,
                    xLiteral = pressX, yLiteral = pressY,
                    xOffsetCoordinateType = CoordinatesType.LITERAL, yOffsetCoordinateType = CoordinatesType.LITERAL,
                    xOffsetLiteral = 0, yOffsetLiteral = 0,
                ),
                GestureFingerMotionEntity(
                    id = moveMotionId.databaseId, fingerId = fingerId.databaseId, index = 1,
                    motionType = GestureFingerMotionType.MOVE,
                    durationMs = moveDurationMs,
                    xCoordinateType = CoordinatesType.LITERAL, yCoordinateType = CoordinatesType.LITERAL,
                    xLiteral = moveToX, yLiteral = moveToY,
                    xOffsetCoordinateType = CoordinatesType.LITERAL, yOffsetCoordinateType = CoordinatesType.LITERAL,
                    xOffsetLiteral = 0, yOffsetLiteral = 0,
                ),
                GestureFingerMotionEntity(
                    id = waitMotionId.databaseId, fingerId = fingerId.databaseId, index = 2,
                    motionType = GestureFingerMotionType.WAIT,
                    durationMs = waitDurationMs,
                ),
                GestureFingerMotionEntity(
                    id = releaseMotionId.databaseId, fingerId = fingerId.databaseId, index = 3,
                    motionType = GestureFingerMotionType.RELEASE,
                ),
            ),
        )

        assertEquals(
            GestureFinger(
                id = fingerId,
                startTimeMs = startTimeMs,
                motions = listOf(
                    GestureFingerMotion.Press(
                        id = pressMotionId,
                        position = GesturePosition.Coordinates(Literal(pressX), Literal(pressY)),
                        offset = GesturePosition.Coordinates(Literal(0), Literal(0)),
                    ),
                    GestureFingerMotion.Move(
                        id = moveMotionId,
                        position = GesturePosition.Coordinates(Literal(moveToX), Literal(moveToY)),
                        offset = GesturePosition.Coordinates(Literal(0), Literal(0)),
                        durationMs = moveDurationMs,
                    ),
                    GestureFingerMotion.Wait(
                        id = waitMotionId,
                        durationMs = waitDurationMs,
                    ),
                    GestureFingerMotion.Release(id = releaseMotionId),
                ),
            ),
            fingerWithMotions.toDomainGestureFinger(),
        )
    }

    @Test
    fun fingerWithMotions_conditionPosition_toDomain() {
        val fingerWithMotions = GestureFingerWithMotions(
            finger = GestureFingerEntity(fingerId.databaseId, actionId, fingerIndex, startTimeMs),
            motions = listOf(
                GestureFingerMotionEntity(
                    id = pressMotionId.databaseId, fingerId = fingerId.databaseId, index = 0,
                    motionType = GestureFingerMotionType.PRESS,
                    xCoordinateType = CoordinatesType.CONDITION, yCoordinateType = CoordinatesType.CONDITION,
                    conditionId = conditionId.databaseId,
                    xOffsetCoordinateType = CoordinatesType.LITERAL, yOffsetCoordinateType = CoordinatesType.LITERAL,
                    xOffsetLiteral = 0, yOffsetLiteral = 0,
                ),
                GestureFingerMotionEntity(
                    id = releaseMotionId.databaseId, fingerId = fingerId.databaseId, index = 1,
                    motionType = GestureFingerMotionType.RELEASE,
                ),
            ),
        )

        assertEquals(
            GestureFinger(
                id = fingerId,
                startTimeMs = startTimeMs,
                motions = listOf(
                    GestureFingerMotion.Press(
                        id = pressMotionId,
                        position = GesturePosition.Condition(conditionId = conditionId),
                        offset = GesturePosition.Coordinates(Literal(0), Literal(0)),
                    ),
                    GestureFingerMotion.Release(id = releaseMotionId),
                ),
            ),
            fingerWithMotions.toDomainGestureFinger(),
        )
    }
}
