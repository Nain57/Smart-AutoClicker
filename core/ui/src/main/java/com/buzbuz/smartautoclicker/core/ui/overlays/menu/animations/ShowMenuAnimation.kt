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
package com.buzbuz.smartautoclicker.core.ui.overlays.menu.animations

import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import androidx.core.view.children

internal class ShowMenuAnimation(
    private val menuView: View,
    private val overlayView: View? = null,
) : MenuAnimation() {

    /** Animation for showing the menu. */
    private val showOverlayMenuAnimation: Animation = createShowAnimation()
    /** Animation for showing the overlayView. */
    private val showOverlayViewAnimation: Animation = createShowAnimation()

    private val onShowOverlayMenuListener = object : AnimationStateListener() {
        override fun onEnd() {
            showOverlayMenuAnimation.setAnimationListener(null)
            if (!areAnimationsRunning()) end()
        }
    }
    private val onShowOverlayViewListener = object : AnimationStateListener() {
        override fun onEnd() {
            showOverlayViewAnimation.setAnimationListener(null)
            if (!areAnimationsRunning()) end()
        }
    }

    override fun onStart() {
        showOverlayMenuAnimation.setAnimationListener(onShowOverlayMenuListener)
        menuView.measure(View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY)
        menuView.startAnimation(showOverlayMenuAnimation)

        if (overlayView is ViewGroup && overlayView.childCount == 1) {
            showOverlayMenuAnimation.setAnimationListener(onShowOverlayViewListener)
            overlayView.children.first().startAnimation(showOverlayViewAnimation)
        }
    }

    override fun onStop() {
        showOverlayMenuAnimation.setAnimationListener(null)
        menuView.clearAnimation()

        showOverlayViewAnimation.setAnimationListener(null)
        overlayView?.clearAnimation()
    }

    private fun areAnimationsRunning(): Boolean =
        onShowOverlayMenuListener.isRunning && onShowOverlayViewListener.isRunning

    private fun createShowAnimation(): Animation = AlphaAnimation(0f, 1f).apply {
        duration = SHOW_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
}

/** Duration of the show overlay menu animation. */
private const val SHOW_ANIMATION_DURATION_MS = 250L