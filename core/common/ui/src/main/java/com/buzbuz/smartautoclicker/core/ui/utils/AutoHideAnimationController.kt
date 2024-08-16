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
package com.buzbuz.smartautoclicker.core.ui.utils

import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import androidx.annotation.AnimRes

import com.buzbuz.smartautoclicker.core.base.extensions.setListener
import com.buzbuz.smartautoclicker.core.ui.R

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoHideAnimationController {

    enum class ScreenSide(@AnimRes internal val inAnim: Int, @AnimRes internal val outAnim: Int) {
        LEFT(R.anim.slide_in_left, R.anim.slide_out_left),
        TOP(R.anim.slide_in_top, R.anim.slide_out_top),
        BOTTOM(R.anim.slide_in_bottom, R.anim.slide_out_bottom),
        RIGHT(R.anim.slide_in_right, R.anim.slide_out_right),
    }

    private lateinit var showAnimation: Animation
    private lateinit var hideAnimation: Animation

    private var autoHideEnabled: Boolean = true
    private var animationScope: CoroutineScope? = null
    private var hideJob: Job? = null
    private var viewToAnimate: View? = null

    fun attachToView(view: View, screenSide: ScreenSide) {
        if (viewToAnimate != null) {
            detachFromView()
        }

        Log.d(TAG, "attaching view $view")
        animationScope = CoroutineScope(Dispatchers.Main)

        showAnimation = AnimationUtils.loadAnimation(view.context, screenSide.inAnim).apply {
            setListener(start = { viewToAnimate?.visibility = View.VISIBLE })
            interpolator = AccelerateDecelerateInterpolator()
        }
        hideAnimation = AnimationUtils.loadAnimation(view.context, screenSide.outAnim).apply {
            setListener(end = { viewToAnimate?.visibility = View.GONE })
            interpolator = AccelerateInterpolator()
        }

        viewToAnimate = view
        if (view.visibility != View.GONE) resetHideCountdown()
    }

    fun detachFromView() {
        Log.d(TAG, "detaching view $viewToAnimate")

        animationScope?.cancel()
        animationScope = null

        viewToAnimate = null
    }

    fun showOrResetTimer() {
        if (hideJob == null) {
            Log.d(TAG, "show view $viewToAnimate")
            viewToAnimate?.startAnimation(showAnimation)
        }
        resetHideCountdown()
    }

    fun hide() {
        if (viewToAnimate?.visibility == View.GONE) return
        Log.d(TAG, "hiding view $viewToAnimate")

        hideJob?.cancel()
        hideJob = null

        viewToAnimate?.startAnimation(hideAnimation)
    }

    fun setAutoHideEnabled(isEnabled: Boolean) {
        if (isEnabled == autoHideEnabled) return
        autoHideEnabled = isEnabled

        if (!isEnabled) {
            hideJob?.cancel()
            hideJob = null

            if (viewToAnimate?.visibility == View.GONE) {
                viewToAnimate?.startAnimation(hideAnimation)
            }
        } else if (viewToAnimate?.visibility == View.VISIBLE) {
            resetHideCountdown()
        }
    }

    private fun resetHideCountdown() {
        if (!autoHideEnabled) return

        Log.d(TAG, "reset hide countdown for view $viewToAnimate")

        hideJob?.cancel()
        hideJob = null

        animationScope?.let { scope ->
            hideJob = scope.launch {
                delay(AUTO_HIDE_TIMER_MS)
                Log.d(TAG, "hiding view after timeout $viewToAnimate")
                viewToAnimate?.startAnimation(hideAnimation)
                hideJob = null
            }
        }
    }
}

private const val TAG = "AutoHideAnimationController"
private const val AUTO_HIDE_TIMER_MS = 3_000L