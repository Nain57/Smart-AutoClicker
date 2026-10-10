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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout

import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.views.gesture.edit.GestureEditorOverlayView
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.GestureRecordView
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.toFingerPath
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.toRecordedFingers
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureFingerPath
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.OverlayDumbGestureEditorMenuBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

import kotlinx.coroutines.launch

/**
 * [OverlayMenu] that renders a dumb gesture on the screen and lets the user reposition its motion
 * waypoints by dragging their circles.
 */
class DumbGestureEditorOverlayMenu(
    private val gesture: DumbAction.DumbGesture,
    private val mode: DumbGestureEditorMode,
) : OverlayMenu() {

    /**
     * Editing mode for [DumbGestureEditorOverlayMenu].
     *
     * [AllMotions] allows interacting with all positions of the selected finger simultaneously, and recording a
     * new path for it. The edited gesture is confirmed via the confirm button and returned through [onGestureEdited].
     *
     * [SingleMotion] restricts interaction to one specific motion position. The position is confirmed via the
     * confirm button and returned through [onPositionConfirmed].
     */
    sealed class DumbGestureEditorMode {

        data class AllMotions(
            val fingerIndex: Int,
            val onGestureEdited: (DumbAction.DumbGesture) -> Unit,
        ) : DumbGestureEditorMode()

        data class SingleMotion(
            val motionId: Identifier,
            val onPositionConfirmed: (Point) -> Unit,
        ) : DumbGestureEditorMode()
    }

    private val viewModel: DumbGestureEditorViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbGestureEditorViewModel() },
    )

    private lateinit var menuBinding: OverlayDumbGestureEditorMenuBinding
    private lateinit var editorView: GestureEditorOverlayView
    private lateinit var gestureRecordView: GestureRecordView

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        menuBinding = OverlayDumbGestureEditorMenuBinding.inflate(layoutInflater).apply {
            btnRecord.isVisible = mode is DumbGestureEditorMode.AllMotions
        }
        return menuBinding.root
    }

    override fun onCreateOverlayView(): View {
        editorView = GestureEditorOverlayView(context).apply {
            isSingleMotionMode = mode is DumbGestureEditorMode.SingleMotion
            when (mode) {
                is DumbGestureEditorMode.AllMotions -> displayedFingerIndex = mode.fingerIndex
                is DumbGestureEditorMode.SingleMotion -> targetMotionId = mode.motionId
            }
            onPositionDragEnded = { fingerIndex, motionId, position ->
                if (mode is DumbGestureEditorMode.AllMotions) {
                    viewModel.updateMotionPosition(fingerIndex, motionId, position.x, position.y)
                }
            }
        }

        gestureRecordView = GestureRecordView(context).apply {
            visibility = View.GONE
        }

        return FrameLayout(context).apply {
            addView(editorView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(gestureRecordView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        if (mode !is DumbGestureEditorMode.AllMotions) return
        if (!isVisible && gestureRecordView.isVisible) stopRecording()
        setMenuItemViewEnabled(menuBinding.btnRecord, isVisible)
    }

    override fun onCreate() {
        super.onCreate()
        viewModel.setInitialGesture(gesture)
        viewModel.setTargetMotionId(
            if (mode is DumbGestureEditorMode.SingleMotion) mode.motionId else null
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { editorView.gesture = it } }
            }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_record -> onRecordClicked()
            R.id.btn_confirm -> onConfirmClicked()
            R.id.btn_cancel -> if (gestureRecordView.isVisible) stopRecording() else back()
        }
    }

    private fun onConfirmClicked() {
        when (val m = mode) {
            is DumbGestureEditorMode.SingleMotion -> {
                editorView.getCurrentPosition(m.motionId)?.let { m.onPositionConfirmed(it) }
                back()
            }
            is DumbGestureEditorMode.AllMotions -> {
                viewModel.getCurrentGesture()?.let { m.onGestureEdited(it) }
                back()
            }
        }
    }

    private fun onRecordClicked() {
        val fingerIndex = (mode as? DumbGestureEditorMode.AllMotions)?.fingerIndex ?: return
        if (gestureRecordView.isVisible) return

        val existingStartTimeMs = editorView.gesture?.fingers?.getOrNull(fingerIndex)?.startTimeMs ?: 0L

        setMenuItemViewEnabled(menuBinding.btnRecord, false)
        setMenuItemViewEnabled(menuBinding.btnConfirm, false)
        setMenuItemViewEnabled(menuBinding.btnHideOverlay, false)
        setMenuItemViewEnabled(menuBinding.btnMove, false)

        editorView.recordingFingerPath = EMPTY_FINGER_PATH.copy(startTimeMs = existingStartTimeMs)

        gestureRecordView.apply {
            maxDurationMs = DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS
            visibility = View.VISIBLE
            gestureCaptureListener = { gesture, isFinished ->
                val firstFinger = gesture?.toRecordedFingers()?.firstOrNull()
                editorView.recordingFingerPath =
                    firstFinger?.toFingerPath(startTimeMs = existingStartTimeMs, isEditable = false)
                        ?: EMPTY_FINGER_PATH.copy(startTimeMs = existingStartTimeMs)
                if (isFinished) {
                    if (firstFinger != null) viewModel.replaceFingerPath(fingerIndex, firstFinger)
                    stopRecording()
                }
            }
        }
    }

    private fun stopRecording() {
        editorView.recordingFingerPath = null
        gestureRecordView.clearAndHide()
        setMenuItemViewEnabled(menuBinding.btnRecord, true)
        setMenuItemViewEnabled(menuBinding.btnConfirm, true)
        setMenuItemViewEnabled(menuBinding.btnHideOverlay, true)
        setMenuItemViewEnabled(menuBinding.btnMove, true)
    }
}

private val EMPTY_FINGER_PATH = GestureFingerPath(waypoints = emptyList())
