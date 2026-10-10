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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFinger as RecordedGestureFinger
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFingerMotion as RecordedGestureFingerMotion
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper.toGestureDescription

import kotlin.math.roundToInt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject


class GestureEditorViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {

    private val targetMotionId: MutableStateFlow<Identifier?> = MutableStateFlow(null)

    private val localGesture: MutableStateFlow<Gesture?> = MutableStateFlow(null)

    private val screenConditions: Flow<List<ScreenCondition>> =
        editionRepository.editionState.editedEventScreenConditionsState
            .map { state -> state.value ?: emptyList() }

    val uiState: StateFlow<GestureDescription?> = combine(
        localGesture,
        screenConditions,
        targetMotionId,
    ) { gesture, conditions, motionId ->
        gesture?.toGestureDescription(conditions, displayConfigManager.displayConfig.sizePx, motionId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setInitialGesture(gesture: Gesture) {
        localGesture.value = gesture
    }

    fun getCurrentGesture(): Gesture? = localGesture.value

    /** Set the motion to edit in single-motion mode. Null means all-motions mode. */
    fun setTargetMotionId(motionId: Identifier?) {
        targetMotionId.value = motionId
    }

    fun updateMotionPosition(fingerIndex: Int, motionId: Identifier, newX: Int, newY: Int) {
        val gesture = localGesture.value ?: return
        val finger = gesture.fingers.getOrNull(fingerIndex) ?: return
        val motionIdx = finger.motions.indexOfFirst { it.id == motionId }
        if (motionIdx == -1) return

        val newPosition = literalPosition(newX, newY)
        val updatedMotion = when (val motion = finger.motions[motionIdx]) {
            is GestureFingerMotion.Press -> motion.copy(position = newPosition)
            is GestureFingerMotion.Move -> motion.copy(position = newPosition)
            else -> return
        }

        localGesture.value = gesture.copy(
            fingers = gesture.fingers.toMutableList().apply {
                set(fingerIndex, finger.copy(
                    motions = finger.motions.toMutableList().apply { set(motionIdx, updatedMotion) }
                ))
            }
        )
    }

    fun replaceFingerPath(fingerIndex: Int, recordedFinger: RecordedGestureFinger) {
        val gesture = localGesture.value ?: return
        val existingFinger = gesture.fingers.getOrNull(fingerIndex) ?: return
        val builder = editionRepository.editedItemsBuilder

        val newMotions = recordedFinger.motions.map { motion ->
            when (motion) {
                is RecordedGestureFingerMotion.Press -> GestureFingerMotion.Press(
                    id = builder.newMotionIdentifier(),
                    position = GesturePosition.Coordinates(
                        x = GestureCoordinatesValue.Literal(motion.position.x.roundToInt()),
                        y = GestureCoordinatesValue.Literal(motion.position.y.roundToInt()),
                    ),
                    offset = GesturePosition.Coordinates(
                        x = GestureCoordinatesValue.Literal(0),
                        y = GestureCoordinatesValue.Literal(0),
                    ),
                )
                is RecordedGestureFingerMotion.Move -> GestureFingerMotion.Move(
                    id = builder.newMotionIdentifier(),
                    position = GesturePosition.Coordinates(
                        x = GestureCoordinatesValue.Literal(motion.moveToPosition.x.roundToInt()),
                        y = GestureCoordinatesValue.Literal(motion.moveToPosition.y.roundToInt()),
                    ),
                    offset = GesturePosition.Coordinates(
                        x = GestureCoordinatesValue.Literal(0),
                        y = GestureCoordinatesValue.Literal(0),
                    ),
                    durationMs = motion.moveToDurationMs,
                )
                is RecordedGestureFingerMotion.Wait -> GestureFingerMotion.Wait(
                    id = builder.newMotionIdentifier(),
                    durationMs = motion.waitDurationMs,
                )
                RecordedGestureFingerMotion.Release -> GestureFingerMotion.Release(
                    id = builder.newMotionIdentifier(),
                )
            }
        }

        localGesture.value = gesture.copy(
            fingers = gesture.fingers.toMutableList().apply {
                set(fingerIndex, existingFinger.copy(motions = newMotions))
            }
        )
    }

    /** Dragging a waypoint always overrides its previous definition, be it a condition or a counter. */
    private fun literalPosition(x: Int, y: Int): GesturePosition =
        GesturePosition.Coordinates(
            x = GestureCoordinatesValue.Literal(x),
            y = GestureCoordinatesValue.Literal(y),
        )
}
