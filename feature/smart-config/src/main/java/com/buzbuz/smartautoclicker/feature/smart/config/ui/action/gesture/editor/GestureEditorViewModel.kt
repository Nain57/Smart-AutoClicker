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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFinger as RecordedGestureFinger
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFingerMotion as RecordedGestureFingerMotion
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypointCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper.toGestureDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper.toWaypointConditions

import dagger.hilt.android.qualifiers.ApplicationContext

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
    @ApplicationContext context: Context,
    bitmapRepository: BitmapRepository,
    private val editionRepository: EditionRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {

    private val targetMotionId: MutableStateFlow<Identifier?> = MutableStateFlow(null)

    private val localGesture: MutableStateFlow<Gesture?> = MutableStateFlow(null)

    private val screenConditions: Flow<List<ScreenCondition>> =
        editionRepository.editionState.editedEventScreenConditionsState
            .map { state -> state.value ?: emptyList() }

    private val conditionOperator: Flow<Int> =
        editionRepository.editionState.editedEventState
            .map { state -> state.value?.conditionOperator ?: AND }

    private val conditionsWithDisplays: Flow<Pair<List<ScreenCondition>, Map<Identifier, GestureWaypointCondition>>> =
        screenConditions.map { conditions -> conditions to conditions.toWaypointConditions(context, bitmapRepository) }

    val uiState: StateFlow<GestureDescription?> = combine(
        localGesture,
        conditionOperator,
        conditionsWithDisplays,
        editionRepository.editionState.allEditedCountersFlow,
        targetMotionId,
    ) { gesture, operator, (conditions, conditionDisplays), counters, motionId ->
        gesture?.toGestureDescription(
            conditionOperator = operator,
            conditions = conditions,
            counters = counters,
            screenSize = displayConfigManager.displayConfig.sizePx,
            singleEditableMotionId = motionId,
            conditionDisplays = conditionDisplays,
        )
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
                    position = Position.Coordinates(
                        x = CoordinatesValue.Literal(motion.position.x.roundToInt()),
                        y = CoordinatesValue.Literal(motion.position.y.roundToInt()),
                    ),
                    offset = Position.Coordinates(
                        x = CoordinatesValue.Literal(0),
                        y = CoordinatesValue.Literal(0),
                    ),
                )
                is RecordedGestureFingerMotion.Move -> GestureFingerMotion.Move(
                    id = builder.newMotionIdentifier(),
                    position = Position.Coordinates(
                        x = CoordinatesValue.Literal(motion.moveToPosition.x.roundToInt()),
                        y = CoordinatesValue.Literal(motion.moveToPosition.y.roundToInt()),
                    ),
                    offset = Position.Coordinates(
                        x = CoordinatesValue.Literal(0),
                        y = CoordinatesValue.Literal(0),
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
    private fun literalPosition(x: Int, y: Int): Position =
        Position.Coordinates(
            x = CoordinatesValue.Literal(x),
            y = CoordinatesValue.Literal(y),
        )
}
