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

import android.util.Log
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.entity.GestureReleaseHoldType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerWithMotions
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.position.counter
import com.buzbuz.smartautoclicker.core.domain.model.action.position.literal
import com.buzbuz.smartautoclicker.core.domain.model.action.position.toCoordinatesType
import com.buzbuz.smartautoclicker.core.domain.model.action.position.xCoordinatesType
import com.buzbuz.smartautoclicker.core.domain.model.action.position.xCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.position.xLiteral
import com.buzbuz.smartautoclicker.core.domain.model.action.position.yCoordinatesType
import com.buzbuz.smartautoclicker.core.domain.model.action.position.yCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.position.yLiteral


internal fun GestureFinger.toFingerEntity(actionId: Long, index: Int): GestureFingerEntity =
    GestureFingerEntity(
        id = id.databaseId,
        actionId = actionId,
        index = index,
        startTimeMs = startTimeMs,
    )

internal fun GestureFingerMotion.toMotionEntity(fingerId: Long, index: Int): GestureFingerMotionEntity =
    when (this) {
        is GestureFingerMotion.Press -> GestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerId,
            index = index,
            motionType = GestureFingerMotionType.PRESS,
            xCoordinateType = position.xCoordinatesType(),
            yCoordinateType = position.yCoordinatesType(),
            conditionId = (position as? Position.Condition)?.conditionId?.databaseId,
            xLiteral = position.xLiteral(),
            xCounter = position.xCounter(),
            yLiteral = position.yLiteral(),
            yCounter = position.yCounter(),
            xOffsetCoordinateType = (offset as? Position.Coordinates)?.x?.toCoordinatesType(),
            yOffsetCoordinateType = (offset as? Position.Coordinates)?.y?.toCoordinatesType(),
            xOffsetLiteral = (offset as? Position.Coordinates)?.x?.literal(),
            xOffsetCounter = (offset as? Position.Coordinates)?.x?.counter(),
            yOffsetLiteral = (offset as? Position.Coordinates)?.y?.literal(),
            yOffsetCounter = (offset as? Position.Coordinates)?.y?.counter(),
        )

        is GestureFingerMotion.Move -> GestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerId,
            index = index,
            motionType = GestureFingerMotionType.MOVE,
            durationMs = durationMs,
            xCoordinateType = position.xCoordinatesType(),
            yCoordinateType = position.yCoordinatesType(),
            conditionId = (position as? Position.Condition)?.conditionId?.databaseId,
            xLiteral = position.xLiteral(),
            xCounter = position.xCounter(),
            yLiteral = position.yLiteral(),
            yCounter = position.yCounter(),
            xOffsetCoordinateType = (offset as? Position.Coordinates)?.x?.toCoordinatesType(),
            yOffsetCoordinateType = (offset as? Position.Coordinates)?.y?.toCoordinatesType(),
            xOffsetLiteral = (offset as? Position.Coordinates)?.x?.literal(),
            xOffsetCounter = (offset as? Position.Coordinates)?.x?.counter(),
            yOffsetLiteral = (offset as? Position.Coordinates)?.y?.literal(),
            yOffsetCounter = (offset as? Position.Coordinates)?.y?.counter(),
        )

        is GestureFingerMotion.Wait -> GestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerId,
            index = index,
            motionType = GestureFingerMotionType.WAIT,
            durationMs = durationMs,
        )

        is GestureFingerMotion.Release -> GestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerId,
            index = index,
            motionType = GestureFingerMotionType.RELEASE,
            holdDurationMs = (hold as? GestureFingerMotion.Release.Hold.ForDuration)?.durationMs,
            releaseHoldType = hold.toReleaseHoldType(),
        )
    }

private fun GestureFingerMotion.Release.Hold.toReleaseHoldType(): GestureReleaseHoldType? =
    when (this) {
        GestureFingerMotion.Release.Hold.None -> null
        GestureFingerMotion.Release.Hold.UntilUnfulfilled -> GestureReleaseHoldType.UNTIL_UNFULFILLED
        GestureFingerMotion.Release.Hold.UntilActionsEnd -> GestureReleaseHoldType.UNTIL_ACTIONS_END
        is GestureFingerMotion.Release.Hold.ForDuration -> GestureReleaseHoldType.FOR_DURATION
    }



internal fun GestureFingerWithMotions.toDomainGestureFinger(cleanIds: Boolean = false): GestureFinger =
    GestureFinger(
        id = Identifier(finger.id, asTemporary = cleanIds),
        startTimeMs = finger.startTimeMs,
        motions = motions
            .sortedBy { it.index }
            .map { it.toDomainGestureMotion(cleanIds) }
    )

private fun GestureFingerMotionEntity.toDomainGestureMotion(cleanIds: Boolean = false): GestureFingerMotion =
    when (motionType) {
        GestureFingerMotionType.PRESS -> GestureFingerMotion.Press(
            id = Identifier(id, asTemporary = cleanIds),
            position = toGesturePosition(cleanIds),
            offset = toGestureOffset(),
        )
        GestureFingerMotionType.MOVE -> GestureFingerMotion.Move(
            id = Identifier(id, asTemporary = cleanIds),
            position = toGesturePosition(cleanIds),
            offset = toGestureOffset(),
            durationMs = durationMs ?: 0L,
        )
        GestureFingerMotionType.WAIT -> GestureFingerMotion.Wait(
            id = Identifier(id, asTemporary = cleanIds),
            durationMs = durationMs ?: 0L,
        )
        GestureFingerMotionType.RELEASE -> GestureFingerMotion.Release(
            id = Identifier(id, asTemporary = cleanIds),
            hold = toDomainReleaseHold(),
        )
    }

private fun GestureFingerMotionEntity.toDomainReleaseHold(): GestureFingerMotion.Release.Hold =
    when (releaseHoldType) {
        null -> GestureFingerMotion.Release.Hold.None
        GestureReleaseHoldType.UNTIL_UNFULFILLED -> GestureFingerMotion.Release.Hold.UntilUnfulfilled
        GestureReleaseHoldType.UNTIL_ACTIONS_END -> GestureFingerMotion.Release.Hold.UntilActionsEnd
        GestureReleaseHoldType.FOR_DURATION -> GestureFingerMotion.Release.Hold.ForDuration(holdDurationMs ?: 0L)
    }

private fun GestureFingerMotionEntity.toGesturePosition(cleanIds: Boolean): Position =
    if (xCoordinateType == yCoordinateType && xCoordinateType == CoordinatesType.CONDITION) {
        Position.Condition(conditionId = conditionId?.let { Identifier(id = it, asTemporary = cleanIds) })
    } else {
        Position.Coordinates(
            x = xCoordinateType.toGestureCoordinatesValue(xLiteral, xCounter),
            y = yCoordinateType.toGestureCoordinatesValue(yLiteral, yCounter),
        )
    }

private fun GestureFingerMotionEntity.toGestureOffset(): Position =
    Position.Coordinates(
        x = xOffsetCoordinateType.toGestureCoordinatesValue(xOffsetLiteral, xOffsetCounter),
        y = yOffsetCoordinateType.toGestureCoordinatesValue(yOffsetLiteral, yOffsetCounter),
    )

private fun CoordinatesType?.toGestureCoordinatesValue(literal: Int?, counter: String?): CoordinatesValue =
    when (this) {
        CoordinatesType.COUNTER -> CoordinatesValue.Counter(counterName = counter ?: "")
        CoordinatesType.LITERAL -> CoordinatesValue.Literal(value = literal ?: 0)
        else -> {
            Log.e(TAG, "Unsupported coordinates type $this; using 0 as value for coordinates")
            CoordinatesValue.Literal(value = 0)
        }
    }

private const val TAG = "GestureFingerMapper"