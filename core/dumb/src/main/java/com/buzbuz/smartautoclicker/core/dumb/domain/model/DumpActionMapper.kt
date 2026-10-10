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
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import android.graphics.Point

import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionWithFingers
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbGestureFingerEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbGestureFingerMotionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbGestureFingerMotionType
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbGestureFingerWithMotions

internal fun DumbActionWithFingers.toDomain(asDomain: Boolean = false): DumbAction = when (action.type) {
    DumbActionType.CLICK -> action.toDomainClick(asDomain)
    DumbActionType.GESTURE -> toDomainGesture(asDomain)
    DumbActionType.PAUSE -> action.toDomainPause(asDomain)
}
internal fun DumbAction.toEntity(scenarioDbId: Long = DATABASE_ID_INSERTION): DumbActionEntity = when (this) {
    is DumbAction.DumbClick -> toClickEntity(scenarioDbId)
    is DumbAction.DumbGesture -> toGestureEntity(scenarioDbId)
    is DumbAction.DumbPause -> toPauseEntity(scenarioDbId)
}

internal fun DumbGestureFinger.toEntity(dumbActionDbId: Long, index: Int): DumbGestureFingerEntity =
    DumbGestureFingerEntity(
        id = id.databaseId,
        dumbActionId = dumbActionDbId,
        index = index,
        startTimeMs = startTimeMs,
    )

internal fun DumbGestureFingerMotion.toEntity(fingerDbId: Long, index: Int): DumbGestureFingerMotionEntity =
    when (this) {
        is DumbGestureFingerMotion.Press -> DumbGestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerDbId,
            index = index,
            motionType = DumbGestureFingerMotionType.PRESS,
            x = position.x,
            y = position.y,
        )
        is DumbGestureFingerMotion.Move -> DumbGestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerDbId,
            index = index,
            motionType = DumbGestureFingerMotionType.MOVE,
            durationMs = durationMs,
            x = position.x,
            y = position.y,
        )
        is DumbGestureFingerMotion.Wait -> DumbGestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerDbId,
            index = index,
            motionType = DumbGestureFingerMotionType.WAIT,
            durationMs = durationMs,
        )
        is DumbGestureFingerMotion.Release -> DumbGestureFingerMotionEntity(
            id = id.databaseId,
            fingerId = fingerDbId,
            index = index,
            motionType = DumbGestureFingerMotionType.RELEASE,
        )
    }

private fun DumbActionEntity.toDomainClick(asDomain: Boolean): DumbAction.DumbClick =
    DumbAction.DumbClick(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name,
        priority = priority,
        position = Point(x!!, y!!),
        pressDurationMs = pressDuration!!,
        repeatCount = repeatCount!!,
        isRepeatInfinite = isRepeatInfinite!!,
        repeatDelayMs = repeatDelay!!,
    )

private fun DumbActionWithFingers.toDomainGesture(asDomain: Boolean): DumbAction.DumbGesture =
    DumbAction.DumbGesture(
        id = Identifier(id = action.id, asTemporary = asDomain),
        scenarioId = Identifier(id = action.dumbScenarioId, asTemporary = asDomain),
        name = action.name,
        priority = action.priority,
        repeatCount = action.repeatCount!!,
        isRepeatInfinite = action.isRepeatInfinite!!,
        repeatDelayMs = action.repeatDelay!!,
        fingers = gestureFingers
            .sortedBy { it.finger.index }
            .map { fingerWithMotions -> fingerWithMotions.toDomain(asDomain) },
    )

private fun DumbGestureFingerWithMotions.toDomain(asDomain: Boolean): DumbGestureFinger =
    DumbGestureFinger(
        id = Identifier(id = finger.id, asTemporary = asDomain),
        startTimeMs = finger.startTimeMs,
        motions = motions
            .sortedBy { it.index }
            .map { motion -> motion.toDomain(asDomain) },
    )

private fun DumbGestureFingerMotionEntity.toDomain(asDomain: Boolean): DumbGestureFingerMotion =
    when (motionType) {
        DumbGestureFingerMotionType.PRESS -> DumbGestureFingerMotion.Press(
            id = Identifier(id = id, asTemporary = asDomain),
            position = Point(x!!, y!!),
        )
        DumbGestureFingerMotionType.MOVE -> DumbGestureFingerMotion.Move(
            id = Identifier(id = id, asTemporary = asDomain),
            position = Point(x!!, y!!),
            durationMs = durationMs!!,
        )
        DumbGestureFingerMotionType.WAIT -> DumbGestureFingerMotion.Wait(
            id = Identifier(id = id, asTemporary = asDomain),
            durationMs = durationMs!!,
        )
        DumbGestureFingerMotionType.RELEASE -> DumbGestureFingerMotion.Release(
            id = Identifier(id = id, asTemporary = asDomain),
        )
    }

private fun DumbActionEntity.toDomainPause(asDomain: Boolean): DumbAction.DumbPause =
    DumbAction.DumbPause(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name,
        priority = priority,
        pauseDurationMs = pauseDuration!!,
    )

private fun DumbAction.DumbClick.toClickEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Click is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name,
        priority = priority,
        type = DumbActionType.CLICK,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        pressDuration = pressDurationMs,
        x = position.x,
        y = position.y,
    )
}

private fun DumbAction.DumbGesture.toGestureEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Gesture is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name,
        priority = priority,
        type = DumbActionType.GESTURE,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
    )
}

private fun DumbAction.DumbPause.toPauseEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Pause is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name,
        priority = priority,
        type = DumbActionType.PAUSE,
        pauseDuration = pauseDurationMs,
    )
}
