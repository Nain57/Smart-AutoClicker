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
package com.buzbuz.smartautoclicker.baseui.overlayviews.condition.selector

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF

import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.extensions.ScreenMetrics
import com.buzbuz.smartautoclicker.ui.R

/**
 * Controls the hints and their animations state.
 *
 * @param context the Android Context.
 * @param styledAttrs
 * @param screenMetrics object providing the current screen size.
 * @param viewInvalidator
 */
class SelectorHintsController(
    context: Context,
    styledAttrs: TypedArray,
    screenMetrics: ScreenMetrics,
    private val viewInvalidator: () -> Unit,
) {

    private companion object {
        /** */
        private const val DEFAULT_HINTS_ICON_MARGIN = 5
        /** */
        private const val DEFAULT_HINTS_ICON_SIZE = 10
    }

    /** The maximum size of the selector. */
    private val maxArea: Rect = RectF().apply {
        val screenSize = screenMetrics.getScreenSize()
        right = screenSize.x.toFloat()
        bottom = screenSize.y.toFloat()
    }.toRect()
    /** */
    private val iconsMargin: Int = styledAttrs.getDimensionPixelSize(
        R.styleable.ConditionSelectorView_hintsIconsMargin,
        DEFAULT_HINTS_ICON_MARGIN
    )
    /** */
    private val iconsSize: Int = styledAttrs.getDimensionPixelSize(
        R.styleable.ConditionSelectorView_hintsIconsSize,
        DEFAULT_HINTS_ICON_SIZE
    )

    /** Map between a gesture type and its hint. */
    private val hintsIcons: Map<GestureType, Hint>

    init {
        val moveIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintMoveIcon, 0)
        val upIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeUpIcon, 0)
        val downIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeDownIcon, 0)
        val leftIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeLeftIcon, 0)
        val rightIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeRightIcon, 0)

        hintsIcons = mapOf(
            Move to HintFactory.buildHint(
                context,
                moveIcon,
                iconsSize,
                maxArea,
                true
            ),
            ResizeBottom to HintFactory.buildHint(
                context,
                intArrayOf(upIcon, downIcon),
                iconsSize,
                iconsMargin,
                maxArea,
                booleanArrayOf(true, false),
                true
            ),
            ResizeTop to HintFactory.buildHint(
                context,
                intArrayOf(upIcon, downIcon),
                iconsSize,
                iconsMargin,
                maxArea,
                booleanArrayOf(false, true),
                true
            ),
            ResizeRight to HintFactory.buildHint(
                context,
                intArrayOf(leftIcon, rightIcon),
                iconsSize,
                iconsMargin,
                maxArea,
                booleanArrayOf(true, false),
                false
            ),
            ResizeLeft to HintFactory.buildHint(
                context,
                intArrayOf(leftIcon, rightIcon),
                iconsSize,
                iconsMargin,
                maxArea,
                booleanArrayOf(false, true),
                false
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
        viewInvalidator.invoke()
    }

    /** Show all hints. */
    fun showAll() {
        alpha = 0
        iconsShown = hintsIcons.keys.toMutableSet()
        viewInvalidator.invoke()
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

        viewInvalidator.invoke()
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
