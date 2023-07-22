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

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator

internal class OverlayMenuAnimations {

    /** Animation for showing the menu. */
    private val showAnimation: Animation = AlphaAnimation(0f, 1f).apply {
        duration = SHOW_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
    var showAnimationIsRunning: Boolean = false
        private set

    /** Animation for hiding the menu. */
    private val hideAnimation: Animation = AlphaAnimation(1f, 0f).apply {
        duration = DISMISS_ANIMATION_DURATION_MS
        interpolator = DecelerateInterpolator()
    }
    var hideAnimationIsRunning: Boolean = false
        private set

    fun startShowAnimation(view: View, onAnimationEnded: () -> Unit) {
        showAnimationIsRunning = true
        showAnimation.setOnEndListener {
            showAnimationIsRunning = false
            onAnimationEnded()
        }

        view.startAnimation(showAnimation)
    }

    fun startHideAnimation(view: View, onAnimationEnded: () -> Unit) {
        hideAnimationIsRunning = true
        hideAnimation.setOnEndListener {
            hideAnimationIsRunning = false
            onAnimationEnded()
        }

        view.startAnimation(hideAnimation)
    }

    private fun Animation.setOnEndListener(end: () -> Unit) {
        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) = Unit
            override fun onAnimationRepeat(animation: Animation?) = Unit
            override fun onAnimationEnd(animation: Animation?) { end() }
        })
    }
}

/** Duration of the show overlay menu animation. */
private const val SHOW_ANIMATION_DURATION_MS = 250L
/** Duration of the dismiss overlay menu animation. */
private const val DISMISS_ANIMATION_DURATION_MS = 125L