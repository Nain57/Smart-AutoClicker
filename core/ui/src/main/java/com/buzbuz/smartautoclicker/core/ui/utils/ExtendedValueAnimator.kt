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

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import androidx.core.animation.doOnEnd

class ExtendedValueAnimator private constructor(): ValueAnimator() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    private var currentRepeatCount: Int = 0
    private var endRequested: Boolean = false

    private var repeatMode: Int = 0
    override fun getRepeatMode(): Int = repeatMode
    override fun setRepeatMode(value: Int) { repeatMode = value }

    private var repeatCount: Int = 0
    override fun getRepeatCount(): Int = repeatCount
    override fun setRepeatCount(value: Int) { repeatCount = value }

    private var startDelay: Long = 0L
    override fun getStartDelay(): Long = startDelay
    override fun setStartDelay(value: Long) { startDelay = value }

    var repeatDelay: Long = 0L
    var reverseDelay: Long = 0L

    init { doOnEnd { onAnimationEnded() } }

    override fun start() {
        endRequested = false
        currentRepeatCount = 0
        startAnimation(startDelay)
    }

    override fun cancel() {
        stop()
        super.cancel()
    }

    override fun end() {
        stop()
        super.end()
    }

    private fun stop() {
        endRequested = true
    }

    private fun onAnimationEnded() {
        if (endRequested) return

        currentRepeatCount++
        if (currentRepeatCount >= repeatCount && repeatCount != INFINITE) return

        when (repeatMode) {
            REVERSE ->
                if (isCurrentRepeatAReverse()) reverseAnimation()
                else startAnimation(repeatDelay)

            RESTART -> startAnimation(repeatDelay)
        }
    }

    private fun startAnimation(delay: Long) {
        if (delay > 0) handler.postDelayed({ super.start() }, delay)
        else super.start()
    }

    private fun reverseAnimation() {
        if (reverseDelay > 0) handler.postDelayed({ reverse() }, reverseDelay)
        else reverse()
    }

    private fun isCurrentRepeatAReverse(): Boolean =
        currentRepeatCount % 2 == 1

    companion object {

        /**
         * Constructs and returns a ExtendedValueAnimator that animates between float values. A single
         * value implies that that value is the one being animated to. However, this is not typically
         * useful in a ValueAnimator object because there is no way for the object to determine the
         * starting value for the animation (unlike ObjectAnimator, which can derive that value
         * from the target object and property being animated). Therefore, there should typically
         * be two or more values.
         *
         * @param values A set of values that the animation will animate between over time.
         * @return A ValueAnimator object that is set up to animate between the given values.
         */
        fun ofFloat(vararg values: Float): ExtendedValueAnimator {
            val anim = ExtendedValueAnimator()
            anim.setFloatValues(*values)
            return anim
        }
    }
}