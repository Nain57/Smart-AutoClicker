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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.utils.AutoHideAnimationController
import com.buzbuz.smartautoclicker.core.ui.utils.PositionPagerSnapHelper
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.gesturerecord.toActionDescription

abstract class ItemBriefMenu(
    @StyleRes theme: Int? = null,
    @StringRes private val noItemText : Int,
) : OverlayMenu(theme = theme, recreateOverlayViewOnRotation = true) {


    /** Layout manager for the recycler view. */
    private val itemListSnapHelper: PositionPagerSnapHelper = PositionPagerSnapHelper()

    /** Controls the action brief panel in and out animations. */
    private lateinit var briefPanelAnimationController: AutoHideAnimationController
    /** Controls the instructions in and out animations. */
    private lateinit var instructionsAnimationController: AutoHideAnimationController
    /** Layout manager for the actions recycler view. */
    private lateinit var recyclerViewLayoutManager: LinearLayoutManagerExt
    /** The view binding for the position selector. */
    protected lateinit var briefViewBinding: ItemsBriefOverlayViewBinding
    /** Adapter displaying the items in the list. */
    private lateinit var briefAdapter: ItemBriefAdapter

    protected open fun onOverlayViewCreated(binding: ItemsBriefOverlayViewBinding): Unit = Unit
    protected open fun onMoveItem(from: Int, to: Int) = Unit
    protected open fun onItemBriefClicked(index: Int, item: ItemBrief): Unit = Unit
    protected open fun onItemPositionCardClicked(index: Int, itemCount: Int): Unit = Unit

    protected abstract fun onDeleteItem(index: Int)
    protected abstract fun onPlayItem(index: Int)
    protected abstract fun onCreateBriefItemViewHolder(parent: ViewGroup, orientation: Int): ItemBriefViewHolder<*>

    override fun onCreateOverlayView(): View {
        briefPanelAnimationController = AutoHideAnimationController()
        instructionsAnimationController = AutoHideAnimationController()

        briefViewBinding = ItemsBriefOverlayViewBinding.inflate(
            inflater = context.getSystemService(LayoutInflater::class.java),
            orientation = displayMetrics.orientation,
        )

        briefAdapter = ItemBriefAdapter(displayMetrics, ::onCreateBriefItemViewHolder) { index, brief ->
            debounceUserInteraction { onItemBriefClicked(index, brief) }
        }

        briefViewBinding.apply {
            briefPanelAnimationController.attachToView(
                layoutActionList,
                if (displayMetrics.orientation == Configuration.ORIENTATION_PORTRAIT)
                    AutoHideAnimationController.ScreenSide.BOTTOM
                else
                    AutoHideAnimationController.ScreenSide.LEFT
            )

            instructionsAnimationController.attachToView(
                layoutInstructions,
                AutoHideAnimationController.ScreenSide.TOP,
            )

            listActions.adapter = briefAdapter
            recyclerViewLayoutManager = LinearLayoutManagerExt(
                context,
                displayMetrics.orientation
            )
            recyclerViewLayoutManager.doOnNextLayoutCompleted {
                itemListSnapHelper.snapTo(0)
            }
            listActions.layoutManager = recyclerViewLayoutManager

            itemListSnapHelper.apply {
                onSnapPositionChangeListener = { snapIndex ->
                    onFocusedItemChanged(snapIndex)
                    briefPanelAnimationController.showOrResetTimer()
                }
                attachToRecyclerView(listActions)
            }

            emptyScenarioText.setText(noItemText)

            root.setOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
            }
            buttonPlay.setDebouncedOnClickListener {
                onPlayItem(itemListSnapHelper.snapPosition)
            }
            buttonDelete.setDebouncedOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
                onDeleteItem(itemListSnapHelper.snapPosition)
            }

            buttonMovePrevious.setDebouncedOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
                onMoveItem(itemListSnapHelper.snapPosition, itemListSnapHelper.snapPosition - 1)
            }
            buttonMoveNext.setDebouncedOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
                onMoveItem(itemListSnapHelper.snapPosition, itemListSnapHelper.snapPosition + 1)
            }

            textActionIndex.setDebouncedOnClickListener {
                onItemPositionCardClicked(getFocusedItemIndex(), briefAdapter.itemCount)
            }
        }

        onFocusedItemChanged(0)
        onOverlayViewCreated(briefViewBinding)
        return briefViewBinding.root
    }

    override fun onResume() {
        super.onResume()
        briefPanelAnimationController.showOrResetTimer()
    }

    override fun onDestroy() {
        briefPanelAnimationController.detachFromView()
        instructionsAnimationController.detachFromView()
        super.onDestroy()
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        if (isVisible) briefPanelAnimationController.showOrResetTimer()
    }

    @CallSuper
    protected open fun onFocusedItemChanged(index: Int) {
        updateBriefButtons()
    }

    protected fun hideMoveButtons() {
        briefViewBinding.buttonMovePrevious.visibility = View.GONE
        briefViewBinding.buttonMoveNext.visibility = View.GONE
    }

    protected fun getFocusedItemIndex(): Int =
        itemListSnapHelper.snapPosition

    protected fun getFocusedItemBrief(): ItemBrief =
        briefAdapter.getItem(getFocusedItemIndex())

    protected fun hidePanel(): Unit =
        briefPanelAnimationController.hide()

    protected fun prepareItemInsertion() {
        recyclerViewLayoutManager.doOnNextAddedItem {
            itemListSnapHelper.snapToLast()
        }
    }

    protected fun updateItemList(actions: List<ItemBrief>) {
        briefViewBinding.apply {

            if (actions.isEmpty()) {
                listActions.visibility = View.GONE
                emptyScenarioCard.visibility = View.VISIBLE
            } else {
                listActions.visibility = View.VISIBLE
                emptyScenarioCard.visibility = View.GONE
            }

            recyclerViewLayoutManager.doOnNextLayoutCompleted {
                onFocusedItemChanged(itemListSnapHelper.snapPosition)
            }
            updateBriefButtons()
            briefAdapter.submitList(actions)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun startGestureCapture(onNewAction: (gesture: ItemBriefDescription?, isFinished: Boolean) -> Unit) {
        briefPanelAnimationController.hide()
        instructionsAnimationController.showOrResetTimer()

        briefViewBinding.viewBrief.setDescription(null)

        briefViewBinding.viewRecorder.apply {
            visibility = View.VISIBLE

            var isCaptureStarted = false
            gestureCaptureListener = { gesture, isFinished ->
                if (gesture != null && !isCaptureStarted){
                    isCaptureStarted = true
                    instructionsAnimationController.hide()
                }
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
        briefPanelAnimationController.showOrResetTimer()
        instructionsAnimationController.hide()
    }

    protected fun isGestureCaptureStarted(): Boolean =
        briefViewBinding.viewRecorder.visibility == View.VISIBLE

    private fun updateBriefButtons() {
        briefViewBinding.apply {
            val itemCount = briefAdapter.itemCount
            val index = itemListSnapHelper.snapPosition

            if (itemCount == 0) {
                buttonMovePrevious.isEnabled = false
                buttonMoveNext.isEnabled = false
                buttonPlay.isEnabled = false
                buttonDelete.isEnabled = false
                textActionIndex.text = "0/0"
                textActionIndex.isEnabled = false
            } else {
                buttonMovePrevious.isEnabled = itemListSnapHelper.snapPosition != 0
                buttonMoveNext.isEnabled = itemListSnapHelper.snapPosition != (itemCount - 1)
                buttonPlay.isEnabled = true
                buttonDelete.isEnabled = true
                textActionIndex.text = "${index + 1}/$itemCount"
                textActionIndex.isEnabled = true
            }
        }
    }
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

