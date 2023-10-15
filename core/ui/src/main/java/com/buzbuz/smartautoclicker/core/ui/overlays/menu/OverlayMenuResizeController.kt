/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays.menu

import android.animation.LayoutTransition
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup

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
 * @param windowSizeListener called when the window needs to be resized.
 */
internal class OverlayMenuResizeController(
    private val backgroundViewGroup: ViewGroup,
    private val resizedContainer: ViewGroup,
    private val maximumSize: Size,
    private val windowSizeListener: (size: Size) -> Unit,
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

            runningTransitions.add(OverlayTransition(view.id, transitionType))
        }

        override fun endTransition(
            transition: LayoutTransition?,
            container: ViewGroup?,
            view: View?,
            transitionType: Int
        ) {
            if (view == null) return

            runningTransitions.remove(OverlayTransition(view.id, transitionType))
            if (runningTransitions.isEmpty()) {
                Log.d(TAG, "Layout changes animations completed")

                // The view resize animation is over, restore the window size to wrap the content.
                windowSizeListener(Size(backgroundViewGroup.measuredWidth, backgroundViewGroup.measuredHeight))
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
        windowSizeListener(maximumSize)

        // Execute layout changes that will cause a resize
        layoutChanges()
    }

    /** Release this controller. */
    fun release() {
        resizedContainer.layoutTransition?.removeTransitionListener(transitionListener)
    }
}

private data class OverlayTransition(
    val viewId: Int,
    val transitionType: Int,
)

private const val TAG = "OverlayMenuResizeController"