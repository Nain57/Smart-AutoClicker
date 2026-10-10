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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture

import android.graphics.Point
import android.os.Build

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFinger
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion
import com.buzbuz.smartautoclicker.feature.dumb.config.domain.DumbEditionRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import javax.inject.Inject

class DumbGestureViewModel @Inject constructor(
    private val dumbEditionRepository: DumbEditionRepository,
) : ViewModel() {

    private val _editedDumbGesture: MutableStateFlow<DumbAction.DumbGesture?> = MutableStateFlow(null)

    /** The finger currently edited by the user. */
    private val _selectedFinger: MutableStateFlow<Int> = MutableStateFlow(0)
    val selectedFinger: StateFlow<Int> = _selectedFinger

    /** The set of expanded motions. */
    private val expandedIndexes: MutableStateFlow<Set<Identifier>> = MutableStateFlow(emptySet())

    /** UI state of the DumbGestureDialog. */
    val uiState: StateFlow<DumbGestureDialogUiState> =
        combine(_editedDumbGesture, selectedFinger, expandedIndexes) { gesture, fingerIdx, expItems ->
            DumbGestureDialogUiState(
                canBeSaved = gesture != null && gesture.isValid(),
                uiItems = gesture?.toUiStateItems(fingerIdx, expItems) ?: emptyList(),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(3_000),
            DumbGestureDialogUiState(),
        )

    fun setEditedDumbGesture(gesture: DumbAction.DumbGesture) {
        _editedDumbGesture.value = gesture.copy()
    }

    fun getEditedDumbGesture(): DumbAction.DumbGesture? =
        _editedDumbGesture.value

    fun updateGesture(gesture: DumbAction.DumbGesture) {
        _editedDumbGesture.value = gesture
    }

    fun setActionName(name: String) {
        _editedDumbGesture.update { gesture -> gesture?.copy(name = name) }
    }

    fun setRepeatCount(repeatCount: Int) {
        _editedDumbGesture.update { gesture -> gesture?.copy(repeatCount = repeatCount) }
    }

    fun toggleInfiniteRepeat() {
        _editedDumbGesture.update { gesture ->
            gesture?.copy(
                isRepeatInfinite = !gesture.isRepeatInfinite,
                repeatCount = if (!gesture.isRepeatCountValid()) 1 else gesture.repeatCount,
            )
        }
    }

    fun setRepeatDelay(delayMs: Long) {
        _editedDumbGesture.update { gesture -> gesture?.copy(repeatDelayMs = delayMs) }
    }

    fun setSelectedFinger(fingerIndex: Int) {
        _selectedFinger.value = fingerIndex
    }

    fun addFinger() {
        val gesture = _editedDumbGesture.value ?: return
        val fingerCount = gesture.fingers.size
        if (fingerCount >= DumbAction.DumbGesture.GESTURE_MAXIMUM_FINGER_COUNT) return

        _editedDumbGesture.value = gesture.copy(
            fingers = gesture.fingers + dumbEditionRepository.dumbActionBuilder.createNewDumbGestureFinger()
        )
        setSelectedFinger(fingerCount)
    }

    fun deleteFinger() {
        val gesture = _editedDumbGesture.value ?: return
        if (gesture.fingers.size <= 1) return

        val fingerIdx = selectedFinger.value
        _editedDumbGesture.value = gesture.copy(
            fingers = gesture.fingers.toMutableList().apply { removeAt(fingerIdx) }
        )
        setSelectedFinger(fingerIdx - 1)
    }

    fun setFingerStartTime(newValue: String) {
        val startTimeMs = newValue.toLongOrNull() ?: return
        updateSelectedFinger { finger -> finger.copy(startTimeMs = startTimeMs) }
    }

    fun addMove() {
        val newMotion = dumbEditionRepository.dumbActionBuilder.createNewDumbGestureMotionMove()
        expandedIndexes.update { old -> old + newMotion.id }
        updateSelectedFinger { finger ->
            finger.copy(motions = finger.motions.toMutableList().apply { add(size - 1, newMotion) })
        }
    }

    fun addWait() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val newMotion = dumbEditionRepository.dumbActionBuilder.createNewDumbGestureMotionWait()
        expandedIndexes.update { old -> old + newMotion.id }
        updateSelectedFinger { finger ->
            finger.copy(motions = finger.motions.toMutableList().apply { add(size - 1, newMotion) })
        }
    }

    fun moveMotionUp(item: DumbGestureDialogUiStateItem) { moveMotion(item, -1) }

    fun moveMotionDown(item: DumbGestureDialogUiStateItem) { moveMotion(item, 1) }

    private fun moveMotion(item: DumbGestureDialogUiStateItem, offset: Int) {
        if (item !is DumbGestureDialogUiStateItem.Collapsable) return

        updateSelectedFinger { finger ->
            val motionIdx = finger.motions.indexOfFirst { it.id == item.id }
            if (motionIdx == -1) return@updateSelectedFinger finger

            val targetIdx = motionIdx + offset
            if (targetIdx < 0 || targetIdx >= finger.motions.size) return@updateSelectedFinger finger

            finger.copy(
                motions = finger.motions.toMutableList().apply {
                    val tmp = this[motionIdx]
                    this[motionIdx] = this[targetIdx]
                    this[targetIdx] = tmp
                }
            )
        }
    }

    fun deleteMotion(item: DumbGestureDialogUiStateItem) {
        if (item !is DumbGestureDialogUiStateItem.Collapsable) return

        expandedIndexes.update { old -> old - item.id }
        updateSelectedFinger { finger ->
            finger.copy(motions = finger.motions.filter { motion -> motion.id != item.id })
        }
    }

    fun expandCollapseItem(item: DumbGestureDialogUiStateItem) {
        if (item !is DumbGestureDialogUiStateItem.Collapsable) return

        expandedIndexes.update { old ->
            if (old.contains(item.id)) old - item.id
            else old + item.id
        }
    }

    fun setItemXPosition(item: DumbGestureDialogUiStateItem, newXValue: String) {
        val newX = newXValue.toIntOrNull() ?: 0
        updateMotionPosition(item) { position -> Point(newX, position.y) }
    }

    fun setItemYPosition(item: DumbGestureDialogUiStateItem, newYValue: String) {
        val newY = newYValue.toIntOrNull() ?: 0
        updateMotionPosition(item) { position -> Point(position.x, newY) }
    }

    fun setItemPosition(item: DumbGestureDialogUiStateItem, newValue: Point) {
        updateMotionPosition(item) { newValue }
    }

    fun setItemDurationMs(item: DumbGestureDialogUiStateItem, newDurationMs: String) {
        val durationMs = newDurationMs.toLongOrNull() ?: return
        updateMotionAtItem(item) { motion ->
            when (motion) {
                is DumbGestureFingerMotion.Move -> motion.copy(durationMs = durationMs)
                is DumbGestureFingerMotion.Wait ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) motion.copy(durationMs = durationMs)
                    else motion
                else -> motion
            }
        }
    }

    private fun updateMotionPosition(item: DumbGestureDialogUiStateItem, transform: (Point) -> Point) {
        updateMotionAtItem(item) { motion ->
            when (motion) {
                is DumbGestureFingerMotion.Press -> motion.copy(position = transform(motion.position))
                is DumbGestureFingerMotion.Move -> motion.copy(position = transform(motion.position))
                else -> motion
            }
        }
    }

    private fun updateMotionAtItem(
        item: DumbGestureDialogUiStateItem,
        transform: (DumbGestureFingerMotion) -> DumbGestureFingerMotion,
    ) {
        if (item !is DumbGestureDialogUiStateItem.Collapsable) return

        updateSelectedFinger { finger ->
            val motionIdx = finger.motions.indexOfFirst { motion -> motion.id == item.id }
            if (motionIdx == -1) return@updateSelectedFinger finger

            finger.copy(
                motions = finger.motions.toMutableList().apply {
                    set(motionIdx, transform(finger.motions[motionIdx]))
                }
            )
        }
    }

    private fun updateSelectedFinger(transform: (DumbGestureFinger) -> DumbGestureFinger) {
        val gesture = _editedDumbGesture.value ?: return
        val fingerIdx = selectedFinger.value
        val finger = gesture.fingers.getOrNull(fingerIdx) ?: return

        _editedDumbGesture.value = gesture.copy(
            fingers = gesture.fingers.toMutableList().apply { set(fingerIdx, transform(finger)) }
        )
    }

    private fun DumbAction.DumbGesture.toUiStateItems(
        selectedFinger: Int,
        expandedItems: Set<Identifier>,
    ): List<DumbGestureDialogUiStateItem> {

        val finger = fingers.getOrNull(selectedFinger) ?: return emptyList()

        return buildList {
            add(
                DumbGestureDialogUiStateItem.Header(
                    actionName = name,
                    nameError = name.isEmpty(),
                    repeatCount = repeatCount,
                    repeatCountError = !isRepeatCountValid(),
                    isRepeatInfinite = isRepeatInfinite,
                    repeatDelayMs = repeatDelayMs,
                    repeatDelayError = !isRepeatDelayValid(),
                    fingerIndex = selectedFinger,
                    fingerCount = fingers.size,
                    currentFingerStartTimeMs = finger.startTimeMs,
                    fingerGestureDurationMs = finger.getDurationMs(),
                    fingerErrorState = buildMap {
                        fingers.forEachIndexed { index, finger -> put(index, !finger.isValid()) }
                    },
                )
            )

            finger.motions.forEachIndexed { index, motion ->
                val isCollapsed = !expandedItems.contains(motion.id)

                when (motion) {
                    is DumbGestureFingerMotion.Press -> add(
                        DumbGestureDialogUiStateItem.Press(
                            id = motion.id,
                            isCollapsed = isCollapsed,
                            position = motion.position,
                        )
                    )

                    is DumbGestureFingerMotion.Move -> add(
                        DumbGestureDialogUiStateItem.Move(
                            id = motion.id,
                            isCollapsed = isCollapsed,
                            moveableUp = finger.motions.getOrNull(index - 1) !is DumbGestureFingerMotion.Press,
                            moveableDown = finger.motions.getOrNull(index + 1) !is DumbGestureFingerMotion.Release,
                            position = motion.position,
                            durationMs = motion.durationMs,
                        )
                    )

                    is DumbGestureFingerMotion.Wait -> add(
                        DumbGestureDialogUiStateItem.Wait(
                            id = motion.id,
                            isCollapsed = isCollapsed,
                            moveableUp = finger.motions.getOrNull(index - 1) !is DumbGestureFingerMotion.Press,
                            moveableDown = finger.motions.getOrNull(index + 1) !is DumbGestureFingerMotion.Release,
                            durationMs = motion.durationMs,
                        )
                    )

                    is DumbGestureFingerMotion.Release -> {
                        add(DumbGestureDialogUiStateItem.Add(selectedFinger))
                        add(DumbGestureDialogUiStateItem.Release(selectedFinger))
                    }
                }
            }
        }
    }
}
