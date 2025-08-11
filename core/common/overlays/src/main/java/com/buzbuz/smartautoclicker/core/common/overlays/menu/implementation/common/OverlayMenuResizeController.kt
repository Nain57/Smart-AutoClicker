
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common

import android.animation.LayoutTransition
import android.util.Log
import android.util.Size
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.marginEnd
import androidx.core.view.marginStart

/**
 * Controls the resize of an overlay window.
 *
 * As the root view of an overlay window can't be animated during a resize trigger automatically via the xml attribute
 * animateLayoutChanges, we need to manually handle it.
 * To fix this issue, we use a [maximumSize] for our window, which will be the biggest width and height it can take in
 * all ui states. Before a size transition, the window will be set to this maximum value, allowing the
 * [backgroundViewGroup] and it's content to be correctly animated. Once the auto resize animation is completed, the
 * window size is set back to the size of it's content.
 * For obvious reason, the root view of the window should be transparent, and should only contain one view, the
 * [backgroundViewGroup].
 *
 * @param backgroundViewGroup the view displaying the background of the overlay
 * @param resizedContainer the view originally resized and that triggers the window resizing.
 * @param maximumSize the maximum width and height the window can have between all its states.
 * @param windowResizer called when the window needs to be resized.
 */
internal class OverlayMenuResizeController(
    private val backgroundViewGroup: ViewGroup,
    private val resizedContainer: ViewGroup,
    private val maximumSize: Size,
    private val windowResizer: (size: Size) -> Unit,
) {

    /** True if the window resize animation is running, false if not. */
    var isAnimating: Boolean = false
        private set

    private val runningTransitions: MutableSet<OverlayTransition> = mutableSetOf()

    /** Monitor the transitions triggered by animateLayoutChanges on the [resizedContainer]. */
    private val transitionListener = object : LayoutTransition.TransitionListener {

        override fun startTransition(
            transition: LayoutTransition?,
            container: ViewGroup?,
            view: View?,
            transitionType: Int
        ) {
            if (view == null) return

            Log.d(TAG, "Layout changes animations start for ${view.id}")
            runningTransitions.add(OverlayTransition(view.id, transitionType))
        }

        override fun endTransition(
            transition: LayoutTransition?,
            container: ViewGroup?,
            view: View?,
            transitionType: Int
        ) {
            if (view == null) return

            Log.d(TAG, "Layout changes animations complete for ${view.id}")
            runningTransitions.remove(OverlayTransition(view.id, transitionType))

            if (runningTransitions.isEmpty()) {
                Log.d(TAG, "All layout changes animations completed")

                // The view resize animation is over, restore the window size to wrap the content.
                windowResizer(measureMenuSize())

                isAnimating = false
            }
        }
    }

    init {
        resizedContainer.layoutTransition?.addTransitionListener(transitionListener)
    }

    /**
     * Call this method when doing any view changes that will resize the window.
     * Setup the window size and execute the changes.
     */
    fun animateLayoutChanges(layoutChanges: () -> Unit) {
        if (isAnimating) {
            Log.d(TAG, "Starting layout changes animations, was already animating...")
            layoutChanges()
            return
        }
        isAnimating = true

        Log.d(TAG, "Starting layout changes animations")

        // Freeze window size to expanded size
        windowResizer(maximumSize)

        // Execute layout changes that will cause a resize
        layoutChanges()
    }

    /** Release this controller. */
    fun release() {
        resizedContainer.layoutTransition?.removeTransitionListener(transitionListener)
    }

    fun measureMenuSize(): Size {
        resizedContainer.measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)

        // Get the height of all children + the padding
        val height = resizedContainer.children.fold(0) { acc, child ->
            acc + (if (child.visibility == View.GONE) 0 else child.height)
        } + resizedContainer.paddingTop + resizedContainer.paddingBottom

        val firstChild = (backgroundViewGroup.getChildAt(0) as? ViewGroup)
        val width = if (firstChild == null || firstChild.id == resizedContainer.id) {
            resizedContainer.width
        } else {
            firstChild.children.fold(0) { acc, child ->
                acc + (
                    if (child.visibility == View.GONE) 0
                    else child.width + child.marginStart + child.marginEnd
                )
            }
        }

        return Size(width, height)
    }
}

private data class OverlayTransition(
    val viewId: Int,
    val transitionType: Int,
)

private const val TAG = "OverlayMenuResizeController"