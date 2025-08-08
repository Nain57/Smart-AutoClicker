
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager

import com.buzbuz.smartautoclicker.core.common.overlays.R
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.utils.AutoHideAnimationController
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.gesturerecord.toActionDescription

abstract class ItemBriefMenu(
    @StyleRes theme: Int? = null,
    @StringRes private val noItemText: Int,
    private val initialItemIndex: Int = 0,
) : OverlayMenu(theme = theme, recreateOverlayViewOnRotation = true) {


    /** Layout manager for the recycler view. */
    private val itemListSnapHelper: ItemsBriefSnapHelper = ItemsBriefSnapHelper()

    /** Controls the action brief panel in and out animations. */
    private lateinit var briefPanelAnimationController: AutoHideAnimationController
    /** Controls the instructions in and out animations. */
    private lateinit var instructionsAnimationController: AutoHideAnimationController
    /** The view binding for the position selector. */
    protected lateinit var briefViewBinding: ItemsBriefOverlayViewBinding
    /** Adapter displaying the items in the list. */
    private lateinit var briefAdapter: ItemBriefAdapter

    private lateinit var blinkingAnimator: Animator

    protected open fun onOverlayViewCreated(binding: ItemsBriefOverlayViewBinding): Unit = Unit

    protected abstract fun onCreateBriefItemViewHolder(parent: ViewGroup, orientation: Int): ItemBriefViewHolder<*>
    protected open fun onBriefItemViewBound(index: Int, itemView: View?): Unit = Unit

    protected open fun onItemBriefClicked(index: Int, item: ItemBrief): Unit = Unit
    protected open fun onItemPositionCardClicked(index: Int, itemCount: Int): Unit = Unit
    protected open fun onMoveItemClicked(from: Int, to: Int) = Unit
    protected abstract fun onPlayItemClicked(index: Int)
    protected abstract fun onDeleteItemClicked(index: Int)

    override fun onCreateOverlayView(): View {
        briefPanelAnimationController = AutoHideAnimationController()
        instructionsAnimationController = AutoHideAnimationController()

        briefViewBinding = ItemsBriefOverlayViewBinding.inflate(
            inflater = context.getSystemService(LayoutInflater::class.java),
            orientation = displayConfigManager.displayConfig.orientation,
        )

        briefAdapter = ItemBriefAdapter(
            displayConfigManager = displayConfigManager,
            viewHolderCreator = ::onCreateBriefItemViewHolder,
            itemBoundListener = ::onBriefItemViewBound,
            onItemClickedListener = { index, brief ->
                debounceUserInteraction { onItemBriefClicked(index, brief) }
            },
        )

        briefViewBinding.apply {
            briefPanelAnimationController.attachToView(
                layoutActionList,
                if (displayConfigManager.displayConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
                    AutoHideAnimationController.ScreenSide.BOTTOM
                else
                    AutoHideAnimationController.ScreenSide.LEFT
            )

            instructionsAnimationController.attachToView(
                layoutInstructions,
                AutoHideAnimationController.ScreenSide.TOP,
            )
            blinkingAnimator = AnimatorInflater.loadAnimator(context, R.animator.blinking)

            listActions.adapter = briefAdapter
            itemListSnapHelper.apply {
                onSnapPositionChangeListener = { snapIndex ->
                    onFocusedItemChanged(snapIndex)
                    briefPanelAnimationController.showOrResetTimer()
                }
                attachToRecyclerView(listActions)
                initialItemIndex = this@ItemBriefMenu.initialItemIndex
            }
            listActions.layoutManager = LinearLayoutManager(
                context,
                if (displayConfigManager.displayConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
                    LinearLayoutManager.HORIZONTAL else LinearLayoutManager.VERTICAL,
                false,
            )

            emptyScenarioText.setText(noItemText)

            root.setOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
            }
            buttonPlay.setDebouncedOnClickListener {
                onPlayItemClicked(itemListSnapHelper.snapPosition)
            }
            buttonDelete.setDebouncedOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
                onDeleteItemClicked(itemListSnapHelper.snapPosition)
            }

            buttonMovePrevious.setDebouncedOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
                onMoveItemClicked(itemListSnapHelper.snapPosition, itemListSnapHelper.snapPosition - 1)
            }
            buttonMoveNext.setDebouncedOnClickListener {
                briefPanelAnimationController.showOrResetTimer()
                onMoveItemClicked(itemListSnapHelper.snapPosition, itemListSnapHelper.snapPosition + 1)
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
        updateBriefButtons(briefAdapter.itemCount)
    }

    protected fun setBriefPanelAutoHide(isEnabled: Boolean) {
        briefPanelAnimationController.setAutoHideEnabled(isEnabled)
    }

    protected fun getFocusedItemIndex(): Int =
        itemListSnapHelper.snapPosition

    protected fun getFocusedItemBrief(): ItemBrief? {
        if (briefAdapter.itemCount == 0) return null
        return briefAdapter.getItem(getFocusedItemIndex().coerceIn(0, briefAdapter.itemCount - 1))
    }

    protected fun hidePanel(): Unit =
        briefPanelAnimationController.hide()

    protected fun updateItemList(actions: List<ItemBrief>) {
        briefViewBinding.apply {

            if (actions.isEmpty()) {
                listActions.visibility = View.GONE
                emptyScenarioCard.visibility = View.VISIBLE
            } else {
                listActions.visibility = View.VISIBLE
                emptyScenarioCard.visibility = View.GONE
            }

            briefAdapter.submitList(actions)
            updateBriefButtons(actions.size)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun startGestureCapture(onNewAction: (gesture: ItemBriefDescription?, isFinished: Boolean) -> Unit) {
        briefPanelAnimationController.hide()

        blinkingAnimator.setTarget(briefViewBinding.recordingIcon)
        blinkingAnimator.start()
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
                briefViewBinding.viewBrief.setDescription(
                    newDescription = gesture?.toActionDescription(),
                    animate = isFinished,
                )

                if (isFinished) {
                    stopGestureCapture()
                    onNewAction(gesture?.toActionDescription(), true)
                }
            }
        }
    }

    protected fun stopGestureCapture() {
        blinkingAnimator.end()

        briefViewBinding.viewRecorder.clearAndHide()
        briefPanelAnimationController.showOrResetTimer()
        instructionsAnimationController.hide()
    }

    protected fun isGestureCaptureStarted(): Boolean =
        briefViewBinding.viewRecorder.visibility == View.VISIBLE

    private fun updateBriefButtons(itemCount: Int) {
        briefViewBinding.apply {
            val index = itemListSnapHelper.snapPosition

            if (itemCount == 0) {
                buttonMovePrevious.isEnabled = false
                buttonMoveNext.isEnabled = false
                buttonPlay.isEnabled = false
                buttonDelete.isEnabled = false
                textActionIndex.text = context.getString(
                    R.string.item_brief_items_count,
                    0,
                    0,
                )
                textActionIndex.isEnabled = false
            } else {
                buttonMovePrevious.isEnabled = itemListSnapHelper.snapPosition != 0
                buttonMoveNext.isEnabled = itemListSnapHelper.snapPosition != (itemCount - 1)
                buttonPlay.isEnabled = true
                buttonDelete.isEnabled = true
                textActionIndex.text = context.getString(
                    R.string.item_brief_items_count,
                    index + 1,
                    itemCount,
                )
                textActionIndex.isEnabled = true
            }
        }
    }
}
