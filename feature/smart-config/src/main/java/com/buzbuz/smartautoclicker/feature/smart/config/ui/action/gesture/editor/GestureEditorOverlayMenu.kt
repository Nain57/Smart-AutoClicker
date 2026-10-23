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
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.ui.views.gesture.edit.GestureEditorOverlayView
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.GestureRecordView
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.toFingerPath
import com.buzbuz.smartautoclicker.core.ui.views.gesture.record.toRecordedFingers
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureFingerPath
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayGestureEditorMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import kotlinx.coroutines.launch

/**
 * [OverlayMenu] that renders a gesture on the screen and lets the user reposition its motion
 * waypoints by dragging their circles.
 */
class GestureEditorOverlayMenu(
    private val gesture: Gesture,
    private val mode: GestureEditorMode,
) : OverlayMenu() {

    /**
     * Editing mode for [GestureEditorOverlayMenu].
     *
     * [AllMotions] allows interacting with all editable positions in the gesture simultaneously.
     * Condition-based positions are displayed as fixed. Counter-based coordinates constrain movement
     * to the free axis only (the axis controlled by the counter cannot be changed).
     * Changes are committed to the repository on each drag end.
     *
     * [SingleMotion] restricts interaction to one specific motion position. When the user drags it,
     * any condition- or counter-based position is replaced with the literal coordinates set by the drag.
     * The position is confirmed via the confirm button and returned through [onPositionConfirmed].
     */
    sealed class GestureEditorMode {

        data class AllMotions(
            val fingerIndex: Int,
            val onGestureEdited: (Gesture) -> Unit,
        ) : GestureEditorMode()

        data class SingleMotion(
            val motionId: Identifier,
            val onPositionConfirmed: (Point) -> Unit,
        ) : GestureEditorMode()
    }

    private val viewModel: GestureEditorViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { gestureEditorViewModel() },
    )

    private lateinit var menuBinding: OverlayGestureEditorMenuBinding
    private lateinit var editorView: GestureEditorOverlayView
    private lateinit var gestureRecordView: GestureRecordView

    override fun tutorialMonitoringTag(): String = "GestureEditorOverlayMenu"

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        menuBinding = OverlayGestureEditorMenuBinding.inflate(layoutInflater).apply {
            btnRecord.isVisible = mode is GestureEditorMode.AllMotions
        }
        return menuBinding.root
    }

    override fun onCreateOverlayView(): View {
        editorView = GestureEditorOverlayView(context).apply {
            isSingleMotionMode = mode is GestureEditorMode.SingleMotion
            when (mode) {
                is GestureEditorMode.AllMotions -> displayedFingerIndex = mode.fingerIndex
                is GestureEditorMode.SingleMotion -> targetMotionId = mode.motionId
            }
            onPositionDragEnded = { fingerIndex, motionId, position ->
                if (mode is GestureEditorMode.AllMotions) {
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
        if (mode !is GestureEditorMode.AllMotions) return
        if (!isVisible && gestureRecordView.isVisible) stopRecording()
        setMenuItemViewEnabled(menuBinding.btnRecord, isVisible)
    }

    override fun onCreate() {
        super.onCreate()
        viewModel.setInitialGesture(gesture)
        viewModel.setTargetMotionId(
            if (mode is GestureEditorMode.SingleMotion) mode.motionId else null
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
            is GestureEditorMode.SingleMotion -> {
                editorView.getCurrentPosition(m.motionId)?.let { m.onPositionConfirmed(it) }
                back()
            }
            is GestureEditorMode.AllMotions -> {
                viewModel.getCurrentGesture()?.let { m.onGestureEdited(it) }
                back()
            }
        }
    }

    private fun onRecordClicked() {
        val fingerIndex = (mode as? GestureEditorMode.AllMotions)?.fingerIndex ?: return
        if (gestureRecordView.isVisible) return

        val existingStartTimeMs = editorView.gesture?.fingers?.getOrNull(fingerIndex)?.startTimeMs ?: 0L

        setMenuItemViewEnabled(menuBinding.btnRecord, false)
        setMenuItemViewEnabled(menuBinding.btnConfirm, false)
        setMenuItemViewEnabled(menuBinding.btnHideOverlay, false)
        setMenuItemViewEnabled(menuBinding.btnMove, false)

        editorView.recordingFingerPath = EMPTY_FINGER_PATH.copy(startTimeMs = existingStartTimeMs)

        gestureRecordView.apply {
            startTimeOffsetMs = existingStartTimeMs
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
