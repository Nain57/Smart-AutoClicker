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

internal class HideMenuAnimation(
    private val menuView: View,
    private val overlayView: View? = null,
) : MenuAnimation() {

    /** Animation for showing the menu. */
    private val hideOverlayMenuAnimation: Animation = createHideMenuAnimation()
    private val onHideOverlayMenuListener = object : AnimationStateListener() {
        override fun onEnd() {
            hideOverlayMenuAnimation.setAnimationListener(null)
            if (!areAnimationsRunning()) end()
        }
    }
    /** Animation for showing the overlayView. */
    private val hideOverlayViewAnimation: Animation = createHideMenuAnimation()
    private val onHideOverlayViewListener = object : AnimationStateListener() {
        override fun onEnd() {
            hideOverlayViewAnimation.setAnimationListener(null)
            if (!areAnimationsRunning()) end()
        }
    }

    override fun onStart() {
        hideOverlayMenuAnimation.setAnimationListener(onHideOverlayMenuListener)
        menuView.measure(View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY)
        menuView.startAnimation(hideOverlayMenuAnimation)

        if (overlayView is ViewGroup && overlayView.childCount == 1) {
            hideOverlayViewAnimation.setAnimationListener(onHideOverlayViewListener)
            overlayView.children.first().startAnimation(hideOverlayViewAnimation)
        }
    }

    override fun onStop() {
        hideOverlayMenuAnimation.setAnimationListener(null)
        menuView.clearAnimation()

        hideOverlayViewAnimation.setAnimationListener(null)
        overlayView?.clearAnimation()
    }

    private fun areAnimationsRunning(): Boolean =
        onHideOverlayMenuListener.isRunning && onHideOverlayViewListener.isRunning

    private fun createHideMenuAnimation(): Animation = AlphaAnimation(1f, 0f).apply {
        duration = HIDE_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
}

/** Duration of the dismiss overlay menu animation. */
private const val HIDE_ANIMATION_DURATION_MS = 150L