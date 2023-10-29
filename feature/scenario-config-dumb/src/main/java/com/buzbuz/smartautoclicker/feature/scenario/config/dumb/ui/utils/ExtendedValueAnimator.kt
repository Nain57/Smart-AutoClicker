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

import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd

class ExtendedValueAnimator private constructor(): ValueAnimator() {

    private var maxRepeatCount: Int = 0
    private var currentRepeatCount: Int = 0

    private var endRequested: Boolean = false

    init {
        doOnEnd {
            if (endRequested) {
                endRequested = false
                return@doOnEnd
            }

            if (maxRepeatCount == INFINITE) start()
            else if (currentRepeatCount < maxRepeatCount) {
                currentRepeatCount++
                start()
            }
        }
    }

    override fun getRepeatCount(): Int = maxRepeatCount

    override fun setRepeatCount(value: Int) {
        maxRepeatCount = value
    }

    override fun start() {
        currentRepeatCount = 1
        super.start()
    }

    override fun cancel() {
        endRequested = true
        super.cancel()
    }

    override fun end() {
        endRequested = true
        super.end()
    }

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