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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture

import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedElementState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.toPositionUiState
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


class GestureDialogViewModel @Inject constructor(
    private val bitmapRepository: BitmapRepository,
    private val displayConfigManager: DisplayConfigManager,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    private val configuredGestureState: Flow<EditedElementState<Gesture>> =
        editionRepository.editionState.editedActionState
            .filter { it.value is Gesture }
            .map { it as EditedElementState<Gesture> }

    /** The finger currently edited by the user. */
    private val _selectedFinger: MutableStateFlow<Int> = MutableStateFlow(0)
    val selectedFinger: StateFlow<Int> = _selectedFinger

    /** The set of expanded motions. */
    private val expandedIndexes: MutableStateFlow<Set<Identifier>> = MutableStateFlow(emptySet())

    /** Tells if the user is currently editing an action. If that's not the case, Dialog should be closed. */
    @OptIn(FlowPreview::class)
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000.milliseconds)

    /** UI state of the GestureDialog. */
    val uiState: StateFlow<GestureDialogUiState> =
        combine(configuredGestureState, selectedFinger, expandedIndexes) { state, fingerIdx, expItems ->
            GestureDialogUiState(
                canBeSaved = state.canBeSaved,
                hasChanged = state.hasChanged,
                uiItems = state.value?.toUiStateItems(fingerIdx, expItems) ?: emptyList()
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(3_000),
            GestureDialogUiState(),
        )


    fun hasUnsavedModifications(): Boolean =
        uiState.value.hasChanged

    fun shouldSelectCondition(): Boolean =
        editionRepository.editionState.getEditedEvent()?.conditionOperator?.let { it == AND } ?: false

    fun getConditionsForPosition(): List<ScreenCondition> =
        editionRepository.editionState.getEditedEvent()?.conditions?.filterIsInstance<ScreenCondition>() ?: emptyList()

    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(bitmapRepository, condition, onBitmapLoaded)

    fun getCurrentGesture(): Gesture? =
        editionRepository.editionState.getEditedAction<Gesture>()

    fun updateGesture(gesture: Gesture) {
        editionRepository.updateEditedAction(gesture)
    }

    fun setSelectedFinger(fingerIndex: Int) {
        _selectedFinger.value = fingerIndex
    }

    fun addFinger() {
        editionRepository.editionState.getEditedAction<Gesture>()?.let { gesture ->
            val fingerCount = gesture.fingers.size
            if (fingerCount >= Gesture.GESTURE_MAXIMUM_FINGER_COUNT) return

            editionRepository.updateEditedAction(
                gesture.copy(
                    fingers = gesture.fingers + editionRepository.editedItemsBuilder.createNewGestureFinger()
                )
            )
            setSelectedFinger(fingerCount)
        }
    }

    fun deleteFinger() {
        editionRepository.editionState.getEditedAction<Gesture>()?.let { gesture ->
            if (gesture.fingers.size <= 1) return

            val fingerIdx = selectedFinger.value
            val newFingers = gesture.fingers.toMutableList().apply { removeAt(fingerIdx) }
            editionRepository.updateEditedAction(gesture.copy(fingers = newFingers))

            setSelectedFinger(fingerIdx - 1)
        }
    }

    fun addMove() {
        editionRepository.editionState.getEditedAction<Gesture>()?.let { gesture ->
            val fingerIdx = selectedFinger.value
            val finger = gesture.fingers.getOrNull(fingerIdx) ?: return
            val newMotion = editionRepository.editedItemsBuilder.createNewGestureMotionMove()
            val newMotions = finger.motions.toMutableList().apply { add(size - 1, newMotion) }

            expandedIndexes.update { old -> old + newMotion.id }
            editionRepository.updateEditedAction(
                gesture.copy(
                    fingers = gesture.fingers.toMutableList().apply {
                        set(fingerIdx, finger.copy(motions = newMotions))
                    }
                )
            )
        }
    }
    fun addWait() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        editionRepository.editionState.getEditedAction<Gesture>()?.let { gesture ->
            val fingerIdx = selectedFinger.value
            val finger = gesture.fingers.getOrNull(fingerIdx) ?: return
            val newMotion = editionRepository.editedItemsBuilder.createNewGestureMotionWait()
            val newMotions = finger.motions.toMutableList().apply { add(size - 1, newMotion) }

            expandedIndexes.update { old -> old + newMotion.id }
            editionRepository.updateEditedAction(
                gesture.copy(
                    fingers = gesture.fingers.toMutableList().apply {
                        set(fingerIdx, finger.copy(motions = newMotions))
                    }
                )
            )
        }
    }

    fun moveMotionUp(item: GestureDialogUiStateItem) { moveMotion(item, -1) }

    fun moveMotionDown(item: GestureDialogUiStateItem) { moveMotion(item, 1) }

    private fun moveMotion(item: GestureDialogUiStateItem, offset: Int) {
        if (item !is GestureDialogUiStateItem.Collapsable) return

        val gesture = editionRepository.editionState.getEditedAction<Gesture>() ?: return
        val fingerIdx = selectedFinger.value
        val finger = gesture.fingers.getOrNull(fingerIdx) ?: return
        val motionIdx = finger.motions.indexOfFirst { it.id == item.id }
        if (motionIdx == -1) return

        val targetIdx = motionIdx + offset
        if (targetIdx < 0 || targetIdx >= finger.motions.size) return

        editionRepository.updateEditedAction(
            gesture.copy(
                fingers = gesture.fingers.toMutableList().apply {
                    set(fingerIdx, finger.copy(motions =
                        finger.motions.toMutableList().apply {
                            val tmp = this[motionIdx]
                            this[motionIdx] = this[targetIdx]
                            this[targetIdx] = tmp
                        }
                    ))
                }
            )
        )
    }

    fun deleteMotion(item: GestureDialogUiStateItem) {
        if (item !is GestureDialogUiStateItem.Collapsable) return

        val gesture = editionRepository.editionState.getEditedAction<Gesture>() ?: return
        val fingerIdx = selectedFinger.value
        val finger = gesture.fingers.getOrNull(fingerIdx) ?: return

        expandedIndexes.update { old -> old - item.id }
        editionRepository.updateEditedAction(
            gesture.copy(
                fingers = gesture.fingers.toMutableList().apply {
                    set(fingerIdx, finger.copy(
                        motions = finger.motions.filter { motion -> motion.id != item.id }
                    ))
                }
            )
        )
    }

    fun expandCollapseItem(item: GestureDialogUiStateItem) {
        if (item !is GestureDialogUiStateItem.Collapsable) return

        expandedIndexes.update { old ->
            if (old.contains(item.id)) old - item.id
            else old + item.id
        }
    }

    fun setFingerStartTime(newValue: String) {
        val startTimeMs = newValue.toLongOrNull() ?: return
        editionRepository.editionState.getEditedAction<Gesture>()?.let { gesture ->
            val fingerIdx = selectedFinger.value
            val finger = gesture.fingers.getOrNull(fingerIdx) ?: return
            editionRepository.updateEditedAction(
                gesture.copy(
                    fingers = gesture.fingers.toMutableList().apply {
                        set(fingerIdx, finger.copy(startTimeMs = startTimeMs))
                    }
                )
            )
        }
    }

    fun setActionName(name: String) {
        editionRepository.editionState.getEditedAction<Gesture>()?.let { gesture ->
            editionRepository.updateEditedAction(gesture.copy(name = "" + name))
        }
    }

    fun setItemXPosition(item: GestureDialogUiStateItem, newXValue: String) {
        val newX = GestureCoordinatesValue.Literal(newXValue.toIntOrNull() ?: 0)
        updateMotionMainPosition(item) { pos ->
            when (pos) {
                is GesturePosition.Coordinates -> pos.copy(x = newX)
                is GesturePosition.Condition -> GesturePosition.Coordinates(x = newX, y = GestureCoordinatesValue.Literal(0))
            }
        }
    }

    fun setItemYPosition(item: GestureDialogUiStateItem, newYValue: String) {
        val newY = GestureCoordinatesValue.Literal(newYValue.toIntOrNull() ?: 0)
        updateMotionMainPosition(item) { pos ->
            when (pos) {
                is GesturePosition.Coordinates -> pos.copy(y = newY)
                is GesturePosition.Condition -> GesturePosition.Coordinates(x = GestureCoordinatesValue.Literal(0), y = newY)
            }
        }
    }

    fun setItemPosition(item: GestureDialogUiStateItem, newValue: Point) {
        val newPos = GesturePosition.Coordinates(
            x = GestureCoordinatesValue.Literal(newValue.x),
            y = GestureCoordinatesValue.Literal(newValue.y),
        )
        updateMotionMainPosition(item) { newPos }
    }

    fun setItemCondition(item: GestureDialogUiStateItem, newValue: Condition?) {
        updateMotionMainPosition(item) { GesturePosition.Condition(conditionId = newValue?.id) }
    }

    fun removeItemCondition(item: GestureDialogUiStateItem) {
        val displaySize = displayConfigManager.displayConfig.sizePx
        updateMotionMainPosition(item) {
            GesturePosition.Coordinates(
                x = GestureCoordinatesValue.Literal(displaySize.x / 2),
                y = GestureCoordinatesValue.Literal(displaySize.y / 2),
            )
        }
    }

    fun setItemXCounter(item: GestureDialogUiStateItem, counterName: String) {
        val newX = GestureCoordinatesValue.Counter(counterName)
        updateMotionMainPosition(item) { pos ->
            when (pos) {
                is GesturePosition.Coordinates -> pos.copy(x = newX)
                is GesturePosition.Condition -> GesturePosition.Coordinates(x = newX, y = GestureCoordinatesValue.Literal(0))
            }
        }
    }

    fun setItemYCounter(item: GestureDialogUiStateItem, counterName: String) {
        val newY = GestureCoordinatesValue.Counter(counterName)
        updateMotionMainPosition(item) { pos ->
            when (pos) {
                is GesturePosition.Coordinates -> pos.copy(y = newY)
                is GesturePosition.Condition -> GesturePosition.Coordinates(x = GestureCoordinatesValue.Literal(0), y = newY)
            }
        }
    }

    fun getItemOffset(item: GestureDialogUiStateItem): GesturePosition.Coordinates? {
        if (item !is GestureDialogUiStateItem.Collapsable) return null
        val gesture = editionRepository.editionState.getEditedAction<Gesture>() ?: return null
        val finger = gesture.fingers.getOrNull(selectedFinger.value) ?: return null
        val motion = finger.motions.firstOrNull { it.id == item.id } ?: return null
        val offset = when (motion) {
            is GestureFingerMotion.Press -> motion.offset
            is GestureFingerMotion.Move -> motion.offset
            else -> return null
        }
        return offset as? GesturePosition.Coordinates
    }

    fun setItemOffset(item: GestureDialogUiStateItem, newValue: GesturePosition.Coordinates) {
        updateMotionAtItem(item) { motion ->
            when (motion) {
                is GestureFingerMotion.Press -> motion.copy(offset = newValue)
                is GestureFingerMotion.Move -> motion.copy(offset = newValue)
                else -> motion
            }
        }
    }

    fun setItemDurationMs(item: GestureDialogUiStateItem, newDurationMs: String) {
        val durationMs = newDurationMs.toLongOrNull() ?: return
        updateMotionAtItem(item) { motion ->
            when (motion) {
                is GestureFingerMotion.Move -> motion.copy(durationMs = durationMs)
                is GestureFingerMotion.Wait ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) motion.copy(durationMs = durationMs)
                    else motion
                else -> motion
            }
        }
    }

    private fun updateMotionMainPosition(
        item: GestureDialogUiStateItem,
        transform: (GesturePosition) -> GesturePosition,
    ) {
        updateMotionAtItem(item) { motion ->
            when (motion) {
                is GestureFingerMotion.Press -> motion.copy(position = transform(motion.position))
                is GestureFingerMotion.Move -> motion.copy(position = transform(motion.position))
                else -> motion
            }
        }
    }

    private fun updateMotionAtItem(
        item: GestureDialogUiStateItem,
        transform: (GestureFingerMotion) -> GestureFingerMotion,
    ) {
        if (item !is GestureDialogUiStateItem.Collapsable) return

        val gesture = editionRepository.editionState.getEditedAction<Gesture>() ?: return
        val fingerIdx = selectedFinger.value
        val finger = gesture.fingers.getOrNull(fingerIdx) ?: return
        val motionIdx = finger.motions.indexOfFirst { motion -> motion.id == item.id }
        if (motionIdx == -1) return

        val motion = finger.motions[motionIdx]
        editionRepository.updateEditedAction(
            gesture.copy(
                fingers = gesture.fingers.toMutableList().apply {
                    set(fingerIdx, finger.copy(
                        motions = finger.motions.toMutableList().apply {
                            set(motionIdx, transform(motion))
                        }
                    ))
                }
            )
        )
    }

    private fun Gesture.toUiStateItems(
        selectedFinger: Int,
        expandedItems: Set<Identifier>,
    ): List<GestureDialogUiStateItem> {

        val finger = fingers.getOrNull(selectedFinger) ?: return emptyList()

        return buildList {
            add(
                GestureDialogUiStateItem.Header(
                    actionName = name ?: "",
                    fingerIndex = selectedFinger,
                    fingerCount = fingers.size,
                    currentFingerStartTimeMs = finger.startTimeMs,
                    maxFingerGestureDurationsMs = Gesture.GESTURE_MAXIMUM_DURATION_MS,
                    fingerGestureDurationMs = finger.getDurationMs(),
                    fingerErrorState = buildMap {
                        fingers.forEachIndexed { index, finger -> put(index, !finger.isComplete()) }
                    },
                )
            )

            finger.motions.forEachIndexed { index, motion ->
                val isCollapsed = !expandedItems.contains(motion.id)

                when (motion) {
                    is GestureFingerMotion.Press -> add(
                        GestureDialogUiStateItem.Press(
                            id = motion.id,
                            isCollapsed = isCollapsed,
                            position = motion.position.toPositionUiState(editionRepository),
                            offsetPosition = motion.offset.toPositionUiState(editionRepository),
                        )
                    )

                    is GestureFingerMotion.Move -> add(
                        GestureDialogUiStateItem.Move(
                            id = motion.id,
                            isCollapsed = isCollapsed,
                            moveableUp = finger.motions.getOrNull(index - 1) !is GestureFingerMotion.Press,
                            moveableDown = finger.motions.getOrNull(index + 1) !is GestureFingerMotion.Release,
                            position = motion.position.toPositionUiState(editionRepository),
                            offsetPosition = motion.offset.toPositionUiState(editionRepository),
                            durationMs = motion.durationMs,
                        )
                    )

                    is GestureFingerMotion.Wait -> add(
                        GestureDialogUiStateItem.Wait(
                            id = motion.id,
                            isCollapsed = isCollapsed,
                            moveableUp = finger.motions.getOrNull(index - 1) !is GestureFingerMotion.Press,
                            moveableDown = finger.motions.getOrNull(index + 1) !is GestureFingerMotion.Release,
                            durationMs = motion.durationMs,
                        )
                    )

                    is GestureFingerMotion.Release -> {
                        add(GestureDialogUiStateItem.Add(selectedFinger))
                        add(GestureDialogUiStateItem.Release(selectedFinger))
                    }
                }
            }
        }
    }
}


