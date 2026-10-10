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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.editor

import android.graphics.Point

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.RecordedGestureFinger
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.feature.dumb.config.domain.DumbEditionRepository
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.toGestureDescription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject


class DumbGestureEditorViewModel @Inject constructor(
    private val dumbEditionRepository: DumbEditionRepository,
) : ViewModel() {

    private val targetMotionId: MutableStateFlow<Identifier?> = MutableStateFlow(null)

    private val localGesture: MutableStateFlow<DumbAction.DumbGesture?> = MutableStateFlow(null)

    val uiState: StateFlow<GestureDescription?> = combine(localGesture, targetMotionId) { gesture, motionId ->
        gesture?.toGestureDescription(motionId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setInitialGesture(gesture: DumbAction.DumbGesture) {
        localGesture.value = gesture
    }

    fun getCurrentGesture(): DumbAction.DumbGesture? = localGesture.value

    /** Set the motion to edit in single-motion mode. Null means all-motions mode. */
    fun setTargetMotionId(motionId: Identifier?) {
        targetMotionId.value = motionId
    }

    fun updateMotionPosition(fingerIndex: Int, motionId: Identifier, newX: Int, newY: Int) {
        val gesture = localGesture.value ?: return
        val finger = gesture.fingers.getOrNull(fingerIndex) ?: return
        val motionIdx = finger.motions.indexOfFirst { it.id == motionId }
        if (motionIdx == -1) return

        val newPosition = Point(newX, newY)
        val updatedMotion = when (val motion = finger.motions[motionIdx]) {
            is DumbGestureFingerMotion.Press -> motion.copy(position = newPosition)
            is DumbGestureFingerMotion.Move -> motion.copy(position = newPosition)
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
        val newMotions = dumbEditionRepository.dumbActionBuilder.createNewDumbGestureMotionsFrom(recordedFinger)

        localGesture.value = gesture.copy(
            fingers = gesture.fingers.toMutableList().apply {
                set(fingerIndex, existingFinger.copy(motions = newMotions))
            }
        )
    }
}
