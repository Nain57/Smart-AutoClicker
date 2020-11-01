/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

import androidx.annotation.ColorInt
import androidx.core.graphics.toRect
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.gestures.*

import java.lang.IllegalArgumentException

/**
 * Controls the hints and their animations state.
 *
 * @param context the Android Context.
 * @param iconsSize the size of the hint icons.
 * @param maxArea the area to draw in. Any hint outside this area will be hidden.
 * @param iconsMargin the margin between two hint icons for hints with two icons.
 * @param iconsColor the color for the hint icons.
 * @param hintFadeDuration the duration of the fade out animation on the hints in milliseconds.
 * @param hintAllFadeDelay the delay before starting the fade out animation when showing all the hints in milliseconds.
 * @param view the view the hints are draw in. Used only for invalidation purposes.
 */
class HintsController(
    context: Context,
    iconsSize: Int,
    maxArea: RectF,
    iconsMargin: Int,
    @ColorInt private val iconsColor: Int,
    private val hintFadeDuration: Long,
    private val hintAllFadeDelay: Long,
    private val view: View
) {

    /** Map between a gesture type and its hint. */
    private val hintsIcons: Map<Int, Hint>
    init {
        HintFactory.iconsMargin = iconsMargin
        HintFactory.iconsSize = iconsSize
        HintFactory.maxArea = maxArea.toRect()
        hintsIcons = mapOf(
            MOVE to HintFactory.buildHint(context, R.drawable.ic_hint_move, true),
            RESIZE_BOTTOM to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_up,
                    R.drawable.ic_hint_resize_down
                ), booleanArrayOf(true, false), true
            ),
            RESIZE_TOP to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_up,
                    R.drawable.ic_hint_resize_down
                ), booleanArrayOf(false, true), true
            ),
            RESIZE_RIGHT to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_left,
                    R.drawable.ic_hint_resize_right
                ), booleanArrayOf(true, false), false
            ),
            RESIZE_LEFT to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_left,
                    R.drawable.ic_hint_resize_right
                ), booleanArrayOf(false, true), false
            )
        )
    }

    /** The set of icons currently shown. */
    private var iconsShown: MutableSet<Int> = HashSet()
    /** Animator controlling the alpha of the currently shown icons. */
    private var hintsAnimator: ValueAnimator? = null
    /** The current alpha value to applied to all currently shown icons. */
    private var alpha: Int = 0

    /**
     * Show the hint for the provided gesture.
     *
     * @param gesture the gesture to show the hint of.
     */
    fun show(gesture: Int) {
        show(mutableSetOf(gesture))
    }

    /** Show all hints. */
    fun showAll() {
        show(hintsIcons.keys.toMutableSet(), hintAllFadeDelay)
    }

    /**
     * Show the provided hints.
     *
     * @param hints the hints to be shown.
     * @param fadeDelay the delay (in milliseconds) before starting the fade animation. Default is 0.
     */
    private fun show(hints: MutableSet<Int>, fadeDelay: Long = 0) {
        alpha = 255
        iconsShown = hints

        hintsAnimator?.cancel()
        hintsAnimator = ValueAnimator.ofInt(255, 0).apply {
            startDelay = fadeDelay
            duration = hintFadeDuration
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                alpha = it.animatedValue as Int
                view.invalidate()
            }
            start()
        }

        view.invalidate()
    }

    /**
     * Invalidate all the hints.
     * If the view is visible, [onDraw] will be called at some point in the future.
     *
     * @param selectorArea the current selector area.
     */
    fun invalidate(selectorArea: Rect) {
        val allShown = iconsShown.size == hintsIcons.size
        iconsShown.forEach { hintType ->
            val hint = hintsIcons[hintType]!!
            when(hintType) {
                MOVE -> hint.invalidate(selectorArea, selectorArea.centerX(),
                    selectorArea.centerY(), alpha)
                RESIZE_TOP -> hint.invalidate(selectorArea, selectorArea.centerX(),
                    selectorArea.top, alpha, booleanArrayOf(false, allShown))
                RESIZE_BOTTOM -> hint.invalidate(selectorArea, selectorArea.centerX(),
                    selectorArea.bottom, alpha, booleanArrayOf(allShown, false))
                RESIZE_LEFT -> hint.invalidate(selectorArea, selectorArea.left,
                    selectorArea.centerY(), alpha, booleanArrayOf(false, allShown))
                RESIZE_RIGHT -> hint.invalidate(selectorArea, selectorArea.right,
                    selectorArea.centerY(), alpha, booleanArrayOf(allShown, false))
                else -> throw IllegalArgumentException("Invalid hint type")
            }
        }

    }

    /**
     * Draw the hints on the canvas.
     *
     * @param canvas the canvas to draw into.
     */
    fun onDraw(canvas: Canvas) {
        iconsShown.forEach { hintType ->
            hintsIcons[hintType]!!.draw(canvas)
        }
    }
}
