/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Lifts a dialog content root when the IME is visible.
 *
 * [NavBarDialog] keeps [android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING] so the portrait bottom bar
 * is not parked by system resize; this controller mirrors IME height via translation instead.
 */
internal class NavBarDialogImeLiftController(
    private val liftTarget: View,
    private val insetHost: View,
) {

    private var lastImeLiftPx: Int = 0
    private var imeAnimationRunning: Boolean = false
    private var imeLiftAnimator: ValueAnimator? = null

    private val insetsAnimationCallback =
        object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                    imeAnimationRunning = true
                    imeLiftAnimator?.cancel()
                }
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat {
                applyImeLift(resolveImeLiftPx(insets))
                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                    imeAnimationRunning = false
                    syncImeLiftAnimated()
                }
            }
        }

    fun start() {
        ViewCompat.setWindowInsetsAnimationCallback(insetHost, insetsAnimationCallback)
        ViewCompat.setOnApplyWindowInsetsListener(insetHost) { _, insets ->
            if (!imeAnimationRunning) {
                applyImeLift(resolveImeLiftPx(insets))
            }
            insets
        }
        ViewCompat.requestApplyInsets(insetHost)
    }

    fun stop() {
        imeLiftAnimator?.cancel()
        imeLiftAnimator = null
        ViewCompat.setWindowInsetsAnimationCallback(insetHost, null)
        ViewCompat.setOnApplyWindowInsetsListener(insetHost, null)
        applyImeLift(0)
    }

    private fun syncImeLiftAnimated() {
        val insets = ViewCompat.getRootWindowInsets(insetHost) ?: return
        val target = resolveImeLiftPx(insets)
        if (target == lastImeLiftPx) return

        imeLiftAnimator?.cancel()
        val start = lastImeLiftPx
        imeLiftAnimator = ValueAnimator.ofInt(start, target).apply {
            duration = IME_LIFT_ANIMATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                applyImeLift(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun resolveImeLiftPx(insets: WindowInsetsCompat): Int {
        if (!insets.isVisible(WindowInsetsCompat.Type.ime())) return 0
        return insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    }

    private fun applyImeLift(imeBottomPx: Int) {
        if (imeBottomPx == lastImeLiftPx) return
        lastImeLiftPx = imeBottomPx
        liftTarget.translationY = -imeBottomPx.toFloat()
    }

    private companion object {
        const val IME_LIFT_ANIMATION_MS = 180L
    }
}
