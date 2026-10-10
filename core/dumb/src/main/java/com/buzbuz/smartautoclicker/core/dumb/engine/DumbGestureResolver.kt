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
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.util.Log

import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.model.FIRST_FINGER_INDEX
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFinger
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion


/** Place the single finger of a click on the timeline of [gestureBuilder]. */
internal fun DumbAction.DumbClick.toResolvedFinger(gestureBuilder: AndroidGestureBuilder): ResolvedFinger? =
    gestureBuilder
        .newFinger(index = FIRST_FINGER_INDEX, startTimeMs = 0L)
        .press(position = position, holdMs = pressDurationMs)
        .release()
        .build()

/** Place the fingers of a gesture on the timeline of [gestureBuilder]. A finger that can't be resolved is skipped. */
internal fun DumbAction.DumbGesture.toResolvedFingers(gestureBuilder: AndroidGestureBuilder): List<ResolvedFinger> =
    fingers.mapIndexedNotNull { index, finger -> finger.toResolvedFinger(index, gestureBuilder) }

private fun DumbGestureFinger.toResolvedFinger(index: Int, gestureBuilder: AndroidGestureBuilder): ResolvedFinger? {
    if (!isValid()) {
        Log.i(TAG, "Can't resolve finger $index, finger is invalid")
        return null
    }

    val fingerBuilder = gestureBuilder.newFinger(index, startTimeMs)
    motions.forEach { motion ->
        when (motion) {
            is DumbGestureFingerMotion.Press -> fingerBuilder.press(motion.position)
            is DumbGestureFingerMotion.Move -> fingerBuilder.move(motion.position, motion.durationMs)
            is DumbGestureFingerMotion.Wait -> fingerBuilder.wait(motion.durationMs)
            is DumbGestureFingerMotion.Release -> fingerBuilder.release()
        }
    }

    return fingerBuilder.build()
}

private const val TAG = "DumbGestureResolver"
