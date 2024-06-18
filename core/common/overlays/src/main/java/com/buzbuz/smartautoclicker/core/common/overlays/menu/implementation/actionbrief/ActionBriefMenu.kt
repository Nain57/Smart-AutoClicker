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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.actionbrief

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.utils.AutoHideAnimationController
import com.buzbuz.smartautoclicker.core.ui.utils.PositionPagerSnapHelper

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
            recyclerViewLayoutManager.setNextLayoutCompletionListener {
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
            buttonPrevious.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
                actionListSnapHelper.snapToPrevious()
            }
            buttonNext.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
                actionListSnapHelper.snapToNext()
            }
        }

        onFocusedItemChanged(0)
        onOverlayViewCreated(briefViewBinding)
        return briefViewBinding.root
    }

    protected open fun onOverlayViewCreated(binding: ActionBriefBinding): Unit = Unit

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
        briefViewBinding.apply {
            val itemCount = getAdapter<Any>().itemCount
            if (itemCount == 0) {
                buttonPrevious.isEnabled = false
                buttonNext.isEnabled = false
            } else {
                buttonPrevious.isEnabled = actionListSnapHelper.snapPosition != 0
                buttonNext.isEnabled = actionListSnapHelper.snapPosition != (itemCount - 1)
            }
        }
    }

    protected fun getFocusedItemIndex(): Int =
        actionListSnapHelper.snapPosition

    protected fun hidePanel(): Unit =
        actionBriefPanelAnimationController.hide()

    protected fun prepareItemInsertion() {
        val index = actionListSnapHelper.snapPosition + 1
        recyclerViewLayoutManager.setNextLayoutCompletionListener {
            actionListSnapHelper.snapTo(index)
        }
    }

    protected fun <T> updateActionList(actions: List<T>) {
        briefViewBinding.apply {

            getAdapter<T>().submitList(actions)

            if (actions.isEmpty()) {
                listActions.visibility = View.GONE
                emptyScenarioCard.visibility = View.VISIBLE
            } else {
                listActions.visibility = View.VISIBLE
                emptyScenarioCard.visibility = View.GONE
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

    fun setNextLayoutCompletionListener(listener: () -> Unit) {
        nextLayoutCompletionListener = listener
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        if (nextLayoutCompletionListener != null) {
            nextLayoutCompletionListener?.invoke()
            nextLayoutCompletionListener = null
        }
    }
}