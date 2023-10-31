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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.utils

import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import com.buzbuz.smartautoclicker.core.base.extensions.setListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoHideAnimationController {

    private lateinit var showAnimation: Animation
    private lateinit var hideAnimation: Animation

    private var animationScope: CoroutineScope? = null
    private var hideJob: Job? = null
    private var viewToAnimate: View? = null

    fun attachToView(view: View) {
        if (viewToAnimate != null) return

        animationScope = CoroutineScope(Dispatchers.Main)

        showAnimation = AnimationUtils.loadAnimation(view.context, R.anim.slide_in).apply {
            setListener(start = { viewToAnimate?.visibility = View.VISIBLE })
            interpolator = AccelerateDecelerateInterpolator()
        }
        hideAnimation = AnimationUtils.loadAnimation(view.context, R.anim.slide_out).apply {
            setListener(end = { viewToAnimate?.visibility = View.GONE })
            interpolator = AccelerateInterpolator()
        }

        viewToAnimate = view

        resetHideCountdown()
    }

    fun detachFromView() {
        animationScope?.cancel()
        animationScope = null

        viewToAnimate = null
    }

    fun showOrResetTimer() {
        if (hideJob == null) {
            Log.d(TAG, "show view")
            viewToAnimate?.startAnimation(showAnimation)
        }

        resetHideCountdown()
    }
    private fun resetHideCountdown() {
        Log.d(TAG, "reset hide countdown")

        hideJob?.cancel()
        hideJob = null

        animationScope?.let { scope ->
            hideJob = scope.launch {
                delay(AUTO_HIDE_TIMER_MS)
                viewToAnimate?.startAnimation(hideAnimation)
                hideJob = null
            }
        }
    }
}

private const val TAG = "AutoHideAnimationController"
private const val AUTO_HIDE_TIMER_MS = 3_000L