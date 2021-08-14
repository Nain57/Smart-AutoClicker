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
package com.buzbuz.smartautoclicker.baseui.selector.condition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

import androidx.annotation.ColorInt
import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.extensions.ScreenMetrics
import com.buzbuz.smartautoclicker.ui.R

/**
 * Controls the hints and their animations state.
 *
 * @param context the Android Context.
 * @param iconsSize the size of the hint icons.
 * @param screenMetrics object providing the current screen size.
 * @param iconsMargin the margin between two hint icons for hints with two icons.
 * @param iconsColor the color for the hint icons.
 * @param hintFadeDuration the duration of the fade out animation on the hints in milliseconds.
 * @param hintAllFadeDelay the delay before starting the fade out animation when showing all the hints in milliseconds.
 * @param view the view the hints are draw in. Used only for invalidation purposes.
 */
class SelectorHintsController(
    context: Context,
    iconsSize: Int,
    screenMetrics: ScreenMetrics,
    iconsMargin: Int,
    @ColorInt private val iconsColor: Int,
    private val hintFadeDuration: Long,
    private val hintAllFadeDelay: Long,
    private val view: View
) {

    /** The maximum size of the selector. */
    private val maxArea: RectF = RectF().apply {
        val screenSize = screenMetrics.getScreenSize()
        right = screenSize.x.toFloat()
        bottom = screenSize.y.toFloat()
    }

    /** Map between a gesture type and its hint. */
    private val hintsIcons: Map<GestureType, Hint>
    init {
        HintFactory.iconsMargin = iconsMargin
        HintFactory.iconsSize = iconsSize
        HintFactory.maxArea = maxArea.toRect()
        hintsIcons = mapOf(
            Move to HintFactory.buildHint(context, R.drawable.ic_hint_move, true),
            ResizeBottom to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_up,
                    R.drawable.ic_hint_resize_down
                ), booleanArrayOf(true, false), true
            ),
            ResizeTop to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_up,
                    R.drawable.ic_hint_resize_down
                ), booleanArrayOf(false, true), true
            ),
            ResizeRight to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_left,
                    R.drawable.ic_hint_resize_right
                ), booleanArrayOf(true, false), false
            ),
            ResizeLeft to HintFactory.buildHint(
                context, intArrayOf(
                    R.drawable.ic_hint_resize_left,
                    R.drawable.ic_hint_resize_right
                ), booleanArrayOf(false, true), false
            )
        )
    }

    /** The set of icons currently shown. */
    private var iconsShown: MutableSet<GestureType> = HashSet()
    /** */
    private val selectorArea = Rect()
    /** The current alpha value to applied to all currently shown icons. */
    var alpha: Int = 0

    /**
     * Show the hint for the provided gesture.
     *
     * @param gesture the gesture to show the hint of.
     */
    fun show(gesture: GestureType) {
        alpha = 255
        iconsShown = mutableSetOf(gesture)
        view.invalidate()
    }

    /** Show all hints. */
    fun showAll() {
        alpha = 0
        iconsShown = hintsIcons.keys.toMutableSet()
        view.invalidate()
    }

    fun invalidate() {
        invalidate(selectorArea)
    }

    /**
     * Invalidate all the hints and update their positions.
     * If the view is visible, [onDraw] will be called at some point in the future.
     *
     * @param newSelectorArea the current selector area.
     */
    fun invalidate(newSelectorArea: Rect) {
        selectorArea.set(newSelectorArea)

        val allShown = iconsShown.size == hintsIcons.size
        iconsShown.forEach { hintType ->
            val hint = hintsIcons[hintType]!!
            when(hintType) {
                Move -> hint.invalidate(newSelectorArea, newSelectorArea.centerX(),
                    newSelectorArea.centerY(), alpha)
                ResizeTop -> hint.invalidate(newSelectorArea, newSelectorArea.centerX(),
                    newSelectorArea.top, alpha, booleanArrayOf(false, allShown))
                ResizeBottom -> hint.invalidate(newSelectorArea, newSelectorArea.centerX(),
                    newSelectorArea.bottom, alpha, booleanArrayOf(allShown, false))
                ResizeLeft -> hint.invalidate(newSelectorArea, newSelectorArea.left,
                    newSelectorArea.centerY(), alpha, booleanArrayOf(false, allShown))
                ResizeRight -> hint.invalidate(newSelectorArea, newSelectorArea.right,
                    newSelectorArea.centerY(), alpha, booleanArrayOf(allShown, false))
            }
        }

        view.invalidate()
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
