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
package com.buzbuz.smartautoclicker.feature.dumb.config.domain

import android.content.Context
import android.graphics.Point
import android.os.Build

import androidx.annotation.RequiresApi

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.identifier.IdentifierCreator
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFinger
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFinger
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFingerMotion

import kotlin.math.roundToInt

class EditedDumbActionsBuilder(
    private val displayConfigManager: DisplayConfigManager,
) {

    private val dumbActionsIdCreator = IdentifierCreator()
    private val gestureFingerIdCreator = IdentifierCreator()
    private val gestureMotionIdCreator = IdentifierCreator()

    private var dumbScenarioId: Identifier? = null

    internal fun startEdition(scenarioId: Identifier) {
        dumbActionsIdCreator.resetIdCount()
        gestureFingerIdCreator.resetIdCount()
        gestureMotionIdCreator.resetIdCount()
        dumbScenarioId = scenarioId
    }

    internal fun clearState() {
        dumbActionsIdCreator.resetIdCount()
        gestureFingerIdCreator.resetIdCount()
        gestureMotionIdCreator.resetIdCount()
        dumbScenarioId = null
    }

    fun createNewDumbClick(context: Context, position: Point): DumbAction.DumbClick =
        DumbAction.DumbClick(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbClickName(),
            position = position,
            pressDurationMs = context.getDefaultDumbClickDurationMs(),
            repeatCount = context.getDefaultDumbClickRepeatCount(),
            isRepeatInfinite = false,
            repeatDelayMs = context.getDefaultDumbClickRepeatDelay(),
        )

    fun createNewDumbPause(context: Context): DumbAction.DumbPause =
        DumbAction.DumbPause(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbPauseName(),
            pauseDurationMs = context.getDefaultDumbPauseDurationMs(),
        )

    fun createNewDumbGesture(context: Context): DumbAction.DumbGesture =
        DumbAction.DumbGesture(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbGestureName(),
            repeatCount = DEFAULT_GESTURE_REPEAT_COUNT,
            isRepeatInfinite = false,
            repeatDelayMs = DEFAULT_GESTURE_REPEAT_DELAY_MS,
            fingers = listOf(createNewDumbGestureFinger()),
        )

    fun createNewDumbGesture(context: Context, recordedFingers: List<RecordedGestureFinger>): DumbAction.DumbGesture {
        val gestureStartMs = recordedFingers.minOfOrNull { it.pressedAtMs } ?: 0L
        return createNewDumbGesture(context).copy(
            fingers = recordedFingers.map { recordedFinger ->
                DumbGestureFinger(
                    id = gestureFingerIdCreator.generateNewIdentifier(),
                    startTimeMs = recordedFinger.pressedAtMs - gestureStartMs,
                    motions = createNewDumbGestureMotionsFrom(recordedFinger),
                )
            },
        )
    }

    fun createNewDumbActionFrom(from: DumbAction): DumbAction =
        when (from) {
            is DumbAction.DumbClick -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
            is DumbAction.DumbPause -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
            is DumbAction.DumbGesture -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
                fingers = from.fingers.map { finger -> createNewDumbGestureFingerFrom(finger) },
            )
        }

    fun newGestureMotionIdentifier(): Identifier = gestureMotionIdCreator.generateNewIdentifier()

    fun createNewDumbGestureFinger(): DumbGestureFinger =
        DumbGestureFinger(
            id = gestureFingerIdCreator.generateNewIdentifier(),
            startTimeMs = 0,
            motions = listOf(
                DumbGestureFingerMotion.Press(
                    id = gestureMotionIdCreator.generateNewIdentifier(),
                    position = getDisplayCenter(),
                ),
                DumbGestureFingerMotion.Release(
                    id = gestureMotionIdCreator.generateNewIdentifier(),
                ),
            ),
        )

    fun createNewDumbGestureMotionMove(): DumbGestureFingerMotion.Move =
        DumbGestureFingerMotion.Move(
            id = gestureMotionIdCreator.generateNewIdentifier(),
            position = getDisplayCenter(),
            durationMs = DEFAULT_GESTURE_MOTION_DURATION_MS,
        )

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNewDumbGestureMotionWait(): DumbGestureFingerMotion.Wait =
        DumbGestureFingerMotion.Wait(
            id = gestureMotionIdCreator.generateNewIdentifier(),
            durationMs = DEFAULT_GESTURE_MOTION_DURATION_MS,
        )

    fun createNewDumbGestureMotionsFrom(recordedFinger: RecordedGestureFinger): List<DumbGestureFingerMotion> =
        recordedFinger.motions.mapNotNull { motion ->
            when (motion) {
                is RecordedGestureFingerMotion.Press -> DumbGestureFingerMotion.Press(
                    id = gestureMotionIdCreator.generateNewIdentifier(),
                    position = Point(motion.position.x.roundToInt(), motion.position.y.roundToInt()),
                )
                is RecordedGestureFingerMotion.Move -> DumbGestureFingerMotion.Move(
                    id = gestureMotionIdCreator.generateNewIdentifier(),
                    position = Point(motion.moveToPosition.x.roundToInt(), motion.moveToPosition.y.roundToInt()),
                    durationMs = motion.moveToDurationMs,
                )
                // Holding a finger still isn't supported by the accessibility API before Android 8.
                is RecordedGestureFingerMotion.Wait ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) DumbGestureFingerMotion.Wait(
                        id = gestureMotionIdCreator.generateNewIdentifier(),
                        durationMs = motion.waitDurationMs,
                    ) else null
                RecordedGestureFingerMotion.Release -> DumbGestureFingerMotion.Release(
                    id = gestureMotionIdCreator.generateNewIdentifier(),
                )
            }
        }

    private fun createNewDumbGestureFingerFrom(from: DumbGestureFinger): DumbGestureFinger =
        from.copy(
            id = gestureFingerIdCreator.generateNewIdentifier(),
            motions = from.motions.map { motion ->
                when (motion) {
                    is DumbGestureFingerMotion.Press -> motion.copy(id = gestureMotionIdCreator.generateNewIdentifier())
                    is DumbGestureFingerMotion.Move -> motion.copy(id = gestureMotionIdCreator.generateNewIdentifier())
                    is DumbGestureFingerMotion.Wait -> motion.copy(id = gestureMotionIdCreator.generateNewIdentifier())
                    is DumbGestureFingerMotion.Release -> motion.copy(id = gestureMotionIdCreator.generateNewIdentifier())
                }
            },
        )

    private fun getDisplayCenter(): Point =
        displayConfigManager.displayConfig.sizePx.let { size -> Point(size.x / 2, size.y / 2) }

    private fun getEditedScenarioIdOrThrow(): Identifier = dumbScenarioId
        ?: throw IllegalStateException("Can't create items without an edited dumb scenario")
}

private const val DEFAULT_GESTURE_MOTION_DURATION_MS = 250L
private const val DEFAULT_GESTURE_REPEAT_COUNT = 1
private const val DEFAULT_GESTURE_REPEAT_DELAY_MS = 0L