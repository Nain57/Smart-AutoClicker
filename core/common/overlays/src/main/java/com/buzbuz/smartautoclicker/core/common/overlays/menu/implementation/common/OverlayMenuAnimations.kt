
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common

import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator

import androidx.core.view.children
import com.buzbuz.smartautoclicker.core.base.Dumpable

import com.buzbuz.smartautoclicker.core.base.extensions.setListener
import java.io.PrintWriter

internal class OverlayMenuAnimations : Dumpable {

    /** Animation for showing the menu. */
    private val showOverlayMenuAnimation: Animation = AlphaAnimation(0f, 1f).apply {
        duration = SHOW_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
    /** Animation for showing the overlayView. */
    private val showOverlayViewAnimation: Animation = AlphaAnimation(0f, 1f).apply {
        duration = SHOW_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
    var showAnimationIsRunning: Boolean = false
        private set

    /** Animation for hiding the menu. */
    private val hideOverlayMenuAnimation: Animation = AlphaAnimation(1f, 0f).apply {
        duration = DISMISS_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
    /** Animation for showing the overlayView. */
    private val hideOverlayViewAnimation: Animation = AlphaAnimation(1f, 0f).apply {
        duration = DISMISS_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
    var hideAnimationIsRunning: Boolean = false
        private set

    fun startShowAnimation(view: View, overlayView: View? = null, onAnimationEnded: () -> Unit) {
        if (showAnimationIsRunning) return

        Log.d(TAG, "Start show animation on view ${view} with visibility ${view.visibility}")

        showAnimationIsRunning = true
        showOverlayMenuAnimation.setListener(
            end = {
                Log.d(TAG, "Show animation ended")
                showAnimationIsRunning = false
                onAnimationEnded()
            }
        )

        if (hideAnimationIsRunning) {
            Log.d(TAG, "Hide animation is running, stopping it first.")
            hideOverlayMenuAnimation.cancel()
            hideOverlayViewAnimation.cancel()
            hideAnimationIsRunning = false
        }

        view.measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
        view.startAnimation(showOverlayMenuAnimation)
        if (overlayView is ViewGroup && overlayView.childCount == 1) {
            overlayView.children.first().startAnimation(showOverlayViewAnimation)
        }
    }

    fun startHideAnimation(view: View, overlayView: View? = null, onAnimationEnded: () -> Unit) {
        if (hideAnimationIsRunning) return

        Log.d(TAG, "Start hide animation")

        hideAnimationIsRunning = true
        hideOverlayMenuAnimation.setListener(
            end = {
                Log.d(TAG, "Hide animation ended")
                hideAnimationIsRunning = false
                onAnimationEnded()
            }
        )

        if (showAnimationIsRunning) {
            Log.d(TAG, "Show animation is running, stopping it first.")

            showOverlayMenuAnimation.cancel()
            showOverlayViewAnimation.cancel()
            showAnimationIsRunning = false
        }

        view.startAnimation(hideOverlayMenuAnimation)
        if (overlayView is ViewGroup && overlayView.childCount == 1) {
            overlayView.children.first().startAnimation(hideOverlayViewAnimation)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        writer.append(prefix)
            .append("showIsRunning=$showAnimationIsRunning; ")
            .append("hideIsRunning=$hideAnimationIsRunning; ")
            .println()
    }
}

/** Duration of the show overlay menu animation. */
private const val SHOW_ANIMATION_DURATION_MS = 250L
/** Duration of the dismiss overlay menu animation. */
private const val DISMISS_ANIMATION_DURATION_MS = 150L
/** Tag for logs */
private const val TAG = "OverlayMenuAnimations"