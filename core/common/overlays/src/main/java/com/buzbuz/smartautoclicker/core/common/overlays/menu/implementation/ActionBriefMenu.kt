/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayViewActionBriefLandBinding
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayViewActionBriefPortBinding
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.utils.AutoHideAnimationController
import com.buzbuz.smartautoclicker.core.ui.utils.PositionPagerSnapHelper
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionBriefView
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionDescription
import com.buzbuz.smartautoclicker.core.ui.views.gesturerecord.GestureRecordView
import com.buzbuz.smartautoclicker.core.ui.views.gesturerecord.toActionDescription

abstract class ActionBriefMenu(
    @StyleRes theme: Int? = null,
    @StringRes private val noActionsStringRes : Int,
) : OverlayMenu(theme = theme, recreateOverlayViewOnRotation = true) {


    /** Layout manager for the actions recycler view. */
    private val actionListSnapHelper: PositionPagerSnapHelper = PositionPagerSnapHelper()

    /** Controls the action brief panel in and out animations. */
    private lateinit var actionBriefPanelAnimationController: AutoHideAnimationController
    /** Layout manager for the actions recycler view. */
    private lateinit var recyclerViewLayoutManager: LinearLayoutManagerExt
    /** The view binding for the position selector. */
    protected lateinit var briefViewBinding: ActionBriefBinding

    protected abstract fun onCreateAdapter(): ListAdapter<*, *>
    protected open fun onOverlayViewCreated(binding: ActionBriefBinding): Unit = Unit
    protected abstract fun onMoveItem(from: Int, to: Int)
    protected abstract fun onDeleteItem(index: Int)
    protected abstract fun onPlayItem(index: Int)
    protected open fun onItemPositionCardClicked(index: Int, itemCount: Int): Unit = Unit

    override fun onCreateOverlayView(): View {
        actionBriefPanelAnimationController = AutoHideAnimationController()

        briefViewBinding = ActionBriefBinding.inflate(
            inflater = context.getSystemService(LayoutInflater::class.java),
            orientation = displayMetrics.orientation,
        )

        briefViewBinding.apply {
            actionBriefPanelAnimationController.attachToView(
                layoutActionList,
                if (displayMetrics.orientation == Configuration.ORIENTATION_PORTRAIT)
                    AutoHideAnimationController.ScreenSide.BOTTOM
                else
                    AutoHideAnimationController.ScreenSide.LEFT
            )

            listActions.adapter = onCreateAdapter()
            recyclerViewLayoutManager = LinearLayoutManagerExt(
                context,
                displayMetrics.orientation
            )
            recyclerViewLayoutManager.doOnNextLayoutCompleted {
                actionListSnapHelper.snapTo(0)
            }
            listActions.layoutManager = recyclerViewLayoutManager

            actionListSnapHelper.apply {
                onSnapPositionChangeListener = { snapIndex ->
                    onFocusedItemChanged(snapIndex)
                    actionBriefPanelAnimationController.showOrResetTimer()
                }
                attachToRecyclerView(listActions)
            }

            emptyScenarioText.setText(noActionsStringRes)

            root.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
            }
            buttonPlay.setOnClickListener {
                onPlayItem(actionListSnapHelper.snapPosition)
            }
            buttonDelete.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
                onDeleteItem(actionListSnapHelper.snapPosition)
            }

            buttonMovePrevious.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
                debounceUserInteraction {
                    onMoveItem(actionListSnapHelper.snapPosition, actionListSnapHelper.snapPosition - 1)
                }
            }
            buttonMoveNext.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
                debounceUserInteraction {
                    onMoveItem(actionListSnapHelper.snapPosition, actionListSnapHelper.snapPosition + 1)
                }
            }

            textActionIndex.setOnClickListener {
                debounceUserInteraction {
                    onItemPositionCardClicked(getFocusedItemIndex(), getAdapter<Any>().itemCount)
                }
            }
        }

        onFocusedItemChanged(0)
        onOverlayViewCreated(briefViewBinding)
        return briefViewBinding.root
    }

    override fun onResume() {
        super.onResume()
        actionBriefPanelAnimationController.showOrResetTimer()
    }

    override fun onDestroy() {
        actionBriefPanelAnimationController.detachFromView()
        super.onDestroy()
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        if (isVisible) actionBriefPanelAnimationController.showOrResetTimer()
    }

    @CallSuper
    protected open fun onFocusedItemChanged(index: Int) {
        updateBriefButtons()
    }

    protected fun getFocusedItemIndex(): Int =
        actionListSnapHelper.snapPosition

    protected fun hidePanel(): Unit =
        actionBriefPanelAnimationController.hide()

    protected fun prepareItemInsertion() {
        recyclerViewLayoutManager.doOnNextAddedItem {
            actionListSnapHelper.snapToLast()
        }
    }

    protected fun <T> updateActionList(actions: List<T>) {
        briefViewBinding.apply {

            if (actions.isEmpty()) {
                listActions.visibility = View.GONE
                emptyScenarioCard.visibility = View.VISIBLE
            } else {
                listActions.visibility = View.VISIBLE
                emptyScenarioCard.visibility = View.GONE
            }

            recyclerViewLayoutManager.doOnNextLayoutCompleted {
                onFocusedItemChanged(actionListSnapHelper.snapPosition)
            }
            updateBriefButtons()
            getAdapter<T>().submitList(actions)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun startGestureCapture(onNewAction: (gesture: ActionDescription?, isFinished: Boolean) -> Unit) {
        actionBriefPanelAnimationController.hide()
        briefViewBinding.viewBrief.setDescription(null)

        briefViewBinding.viewRecorder.apply {
            visibility = View.VISIBLE
            gestureCaptureListener = { gesture, isFinished ->
                briefViewBinding.viewBrief.setDescription(gesture?.toActionDescription(), isFinished)

                if (isFinished) {
                    stopGestureCapture()
                    onNewAction(gesture?.toActionDescription(), true)
                }
            }
        }
    }

    protected fun stopGestureCapture() {
        briefViewBinding.viewRecorder.clearAndHide()
        actionBriefPanelAnimationController.showOrResetTimer()
    }

    protected fun isGestureCaptureStarted(): Boolean =
        briefViewBinding.viewRecorder.visibility == View.VISIBLE

    private fun updateBriefButtons() {
        briefViewBinding.apply {
            val itemCount = getAdapter<Any>().itemCount
            val index = actionListSnapHelper.snapPosition

            if (itemCount == 0) {
                buttonMovePrevious.isEnabled = false
                buttonMoveNext.isEnabled = false
                buttonPlay.isEnabled = false
                buttonDelete.isEnabled = false
                textActionIndex.text = "0/0"
                textActionIndex.isEnabled = false
            } else {
                buttonMovePrevious.isEnabled = actionListSnapHelper.snapPosition != 0
                buttonMoveNext.isEnabled = actionListSnapHelper.snapPosition != (itemCount - 1)
                buttonPlay.isEnabled = true
                buttonDelete.isEnabled = true
                textActionIndex.text = "${index + 1}/$itemCount"
                textActionIndex.isEnabled = true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getAdapter(): ListAdapter<T, *> =
        briefViewBinding.listActions.adapter as ListAdapter<T, *>
}


private class LinearLayoutManagerExt(context: Context, screenOrientation: Int) : LinearLayoutManager(
    /* context */ context,
    /* orientation */ if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) HORIZONTAL else VERTICAL,
    /* reverseLayout */false,
) {

    private var nextLayoutCompletionListener: (() -> Unit)? = null
    private var nextAddedItemListener: (() -> Unit)? = null
    private var itemCountAtAddedItemRegistration: Int? = null

    fun doOnNextAddedItem(listener: () -> Unit) {
        itemCountAtAddedItemRegistration = itemCount
        nextAddedItemListener = listener
    }

    fun doOnNextLayoutCompleted(listener: () -> Unit) {
        nextLayoutCompletionListener = listener
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)

        val previousCount = itemCountAtAddedItemRegistration
        if (previousCount != null && previousCount < itemCount) {
            nextAddedItemListener?.invoke()
            nextAddedItemListener = null
            itemCountAtAddedItemRegistration = null
        }

        nextLayoutCompletionListener?.invoke()
        nextLayoutCompletionListener = null
    }
}

class ActionBriefBinding private constructor(
    val root: View,
    val viewBrief: ActionBriefView,
    val viewRecorder: GestureRecordView,
    val layoutActionList: View,
    val listActions: RecyclerView,
    val textActionIndex: TextView,
    val buttonMovePrevious: Button,
    val buttonMoveNext: Button,
    val buttonDelete: Button,
    val buttonPlay: Button,
    val emptyScenarioCard: View,
    val emptyScenarioText: TextView,
) {

    companion object {

        fun inflate(inflater: LayoutInflater, orientation: Int) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ActionBriefBinding(OverlayViewActionBriefPortBinding.inflate(inflater))
            else
                ActionBriefBinding(OverlayViewActionBriefLandBinding.inflate(inflater))
    }

    constructor(binding: OverlayViewActionBriefPortBinding) : this(
        root = binding.root,
        viewBrief = binding.viewBrief,
        viewRecorder = binding.viewRecord,
        layoutActionList = binding.layoutActionList,
        listActions = binding.listActions,
        textActionIndex = binding.textActionIndex,
        buttonMovePrevious = binding.buttonMovePrevious,
        buttonMoveNext = binding.buttonMoveNext,
        buttonDelete = binding.buttonDelete,
        buttonPlay = binding.buttonPlayAction,
        emptyScenarioCard = binding.emptyScenarioCard,
        emptyScenarioText = binding.textEmptyScenario,
    )

    constructor(binding: OverlayViewActionBriefLandBinding) : this(
        root = binding.root,
        viewBrief = binding.viewBrief,
        viewRecorder = binding.viewRecord,
        layoutActionList = binding.layoutActionList,
        listActions = binding.listActions,
        textActionIndex = binding.textActionIndex,
        buttonMovePrevious = binding.buttonMovePrevious,
        buttonMoveNext = binding.buttonMoveNext,
        buttonDelete = binding.buttonDelete,
        buttonPlay = binding.buttonPlayAction,
        emptyScenarioCard = binding.emptyScenarioCard,
        emptyScenarioText = binding.textEmptyScenario,
    )
}