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
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerWithMotions
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue.*


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
            conditionId = (position as? GesturePosition.Condition)?.conditionId?.databaseId,
            xLiteral = position.xLiteral(),
            xCounter = position.xCounter(),
            yLiteral = position.yLiteral(),
            yCounter = position.yCounter(),
            xOffsetCoordinateType = (offset as? GesturePosition.Coordinates)?.x?.toCoordinatesType(),
            yOffsetCoordinateType = (offset as? GesturePosition.Coordinates)?.y?.toCoordinatesType(),
            xOffsetLiteral = (offset as? GesturePosition.Coordinates)?.x?.literal(),
            xOffsetCounter = (offset as? GesturePosition.Coordinates)?.x?.counter(),
            yOffsetLiteral = (offset as? GesturePosition.Coordinates)?.y?.literal(),
            yOffsetCounter = (offset as? GesturePosition.Coordinates)?.y?.counter(),
        )

        is GestureFingerMotion.Move -> GestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerId,
            index = index,
            motionType = GestureFingerMotionType.MOVE,
            durationMs = durationMs,
            xCoordinateType = position.xCoordinatesType(),
            yCoordinateType = position.yCoordinatesType(),
            conditionId = (position as? GesturePosition.Condition)?.conditionId?.databaseId,
            xLiteral = position.xLiteral(),
            xCounter = position.xCounter(),
            yLiteral = position.yLiteral(),
            yCounter = position.yCounter(),
            xOffsetCoordinateType = (offset as? GesturePosition.Coordinates)?.x?.toCoordinatesType(),
            yOffsetCoordinateType = (offset as? GesturePosition.Coordinates)?.y?.toCoordinatesType(),
            xOffsetLiteral = (offset as? GesturePosition.Coordinates)?.x?.literal(),
            xOffsetCounter = (offset as? GesturePosition.Coordinates)?.x?.counter(),
            yOffsetLiteral = (offset as? GesturePosition.Coordinates)?.y?.literal(),
            yOffsetCounter = (offset as? GesturePosition.Coordinates)?.y?.counter(),
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
            holdUntilUnfulfilled = holdUntilUnfulfilled,
        )
    }

private fun GesturePosition.xCoordinatesType(): CoordinatesType = when (this) {
    is GesturePosition.Condition -> CoordinatesType.CONDITION
    is GesturePosition.Coordinates -> x.toCoordinatesType()
}

private fun GesturePosition.yCoordinatesType(): CoordinatesType = when (this) {
    is GesturePosition.Condition -> CoordinatesType.CONDITION
    is GesturePosition.Coordinates -> y.toCoordinatesType()
}

private fun GesturePosition.xLiteral(): Int? =
    (this as? GesturePosition.Coordinates)?.x?.literal()

private fun GesturePosition.xCounter(): String? =
    (this as? GesturePosition.Coordinates)?.x?.counter()

private fun GesturePosition.yLiteral(): Int? =
    (this as? GesturePosition.Coordinates)?.y?.literal()

private fun GesturePosition.yCounter(): String? =
    (this as? GesturePosition.Coordinates)?.y?.counter()

private fun GestureCoordinatesValue.toCoordinatesType(): CoordinatesType = when (this) {
    is Literal -> CoordinatesType.LITERAL
    is Counter -> CoordinatesType.COUNTER
}

private fun GestureCoordinatesValue.literal(): Int? = (this as? Literal)?.value
private fun GestureCoordinatesValue.counter(): String? = (this as? Counter)?.counterName

internal fun GestureFingerWithMotions.toDomainGestureFinger(cleanIds: Boolean = false): GestureFinger =
    GestureFinger(
        id = Identifier(finger.id, asTemporary = cleanIds),
        startTimeMs = finger.startTimeMs,
        motions = motions
            .sortedBy { it.index }
            .map { it.toDomainGestureMotion() }
    )

private fun GestureFingerMotionEntity.toDomainGestureMotion(cleanIds: Boolean = false): GestureFingerMotion =
    when (motionType) {
        GestureFingerMotionType.PRESS -> GestureFingerMotion.Press(
            id = Identifier(id, asTemporary = cleanIds),
            position = toGesturePosition(),
            offset = toGestureOffset(),
        )
        GestureFingerMotionType.MOVE -> GestureFingerMotion.Move(
            id = Identifier(id, asTemporary = cleanIds),
            position = toGesturePosition(),
            offset = toGestureOffset(),
            durationMs = durationMs ?: 0L,
        )
        GestureFingerMotionType.WAIT -> GestureFingerMotion.Wait(
            id = Identifier(id, asTemporary = cleanIds),
            durationMs = durationMs ?: 0L,
        )
        GestureFingerMotionType.RELEASE -> GestureFingerMotion.Release(
            id = Identifier(id, asTemporary = cleanIds),
            holdUntilUnfulfilled = holdUntilUnfulfilled == true,
        )
    }

private fun GestureFingerMotionEntity.toGesturePosition(): GesturePosition =
    if (xCoordinateType == yCoordinateType && xCoordinateType == CoordinatesType.CONDITION) {
        GesturePosition.Condition(conditionId = conditionId?.let { Identifier(databaseId = it) })
    } else {
        GesturePosition.Coordinates(
            x = toGestureCoordinatesValue(xCoordinateType, xLiteral, xCounter),
            y = toGestureCoordinatesValue(yCoordinateType, yLiteral, yCounter),
        )
    }

private fun GestureFingerMotionEntity.toGestureOffset(): GesturePosition =
    GesturePosition.Coordinates(
        x = toGestureCoordinatesValue(xOffsetCoordinateType, xOffsetLiteral, xOffsetCounter),
        y = toGestureCoordinatesValue(yOffsetCoordinateType, yOffsetLiteral, yOffsetCounter),
    )

private fun toGestureCoordinatesValue(type: CoordinatesType?, literal: Int?, counter: String?): GestureCoordinatesValue =
    when (type) {
        CoordinatesType.COUNTER -> Counter(counterName = counter ?: "")
        CoordinatesType.LITERAL -> Literal(value = literal ?: 0)
        else -> {
            Log.e(TAG, "Unsupported coordinates type $type; using 0 as value for coordinates")
            Literal(value = 0)
        }
    }

private const val TAG = "GestureFingerMapper"