/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.condition.hints

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent

import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.core.ui.views.ConditionSelectorView
import com.buzbuz.smartautoclicker.core.ui.views.condition.ViewComponent
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.views.condition.GestureType
import com.buzbuz.smartautoclicker.core.ui.views.condition.MoveSelector
import com.buzbuz.smartautoclicker.core.ui.views.condition.ResizeBottom
import com.buzbuz.smartautoclicker.core.ui.views.condition.ResizeLeft
import com.buzbuz.smartautoclicker.core.ui.views.condition.ResizeRight
import com.buzbuz.smartautoclicker.core.ui.views.condition.ResizeTop
import com.buzbuz.smartautoclicker.core.ui.views.condition.ZoomCapture
import com.buzbuz.smartautoclicker.core.ui.R

/**
 * Controls the hints and their animations state.
 *
 * @param context the Android Context.
 * @param styledAttrs the styled attributes of the [ConditionSelectorView]
 * @param displayMetrics object providing the current screen size.
 * @param viewInvalidator calls invalidate on the view hosting this component.
 */
internal class HintsComponent(
    context: Context,
    styledAttrs: TypedArray,
    displayMetrics: DisplayMetrics,
    viewInvalidator: () -> Unit,
): ViewComponent(displayMetrics, viewInvalidator) {

    /** The distance between the hint and the selector border. */
    private val iconsMargin: Int = styledAttrs.getDimensionPixelSize(
        R.styleable.ConditionSelectorView_hintsIconsMargin,
        DEFAULT_HINTS_ICON_MARGIN
    )
    /** The size of the icons for the hints. */
    private val iconsSize: Int = styledAttrs.getDimensionPixelSize(
        R.styleable.ConditionSelectorView_hintsIconsSize,
        DEFAULT_HINTS_ICON_SIZE
    )
    /** The margin between the top of the selector and the pinch hint. */
    private val pinchIconMargin: Int = styledAttrs.getDimensionPixelSize(
        R.styleable.ConditionSelectorView_hintsPinchIconMargin,
        DEFAULT_HINTS_PINCH_ICON_MARGIN
    )

    /** Map between a gesture type and its hint. */
    private val hintsIcons: Map<GestureType, Hint>
    /** Maximum area for the hints. */
    private val maxRect = Rect()

    /** Initialize the list of hints. */
    init {
        val moveIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintMoveIcon, 0)
        val pinchIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintPinchIcon, 0)
        val upIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeUpIcon, 0)
        val downIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeDownIcon, 0)
        val leftIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeLeftIcon, 0)
        val rightIcon = styledAttrs.getResourceId(R.styleable.ConditionSelectorView_hintResizeRightIcon, 0)
        maxRect.set(maxArea.toRect())

        hintsIcons = mapOf(
            MoveSelector to SingleHint(
                context,
                iconsSize,
                maxRect,
                moveIcon,
                booleanArrayOf(true)
            ),
            ZoomCapture to SingleHint(
                context,
                iconsSize,
                maxRect,
                pinchIcon,
                booleanArrayOf(false)
            ),
            ResizeBottom to DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(upIcon, downIcon),
                booleanArrayOf(true, false),
                true
            ),
            ResizeTop to DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(upIcon, downIcon),
                booleanArrayOf(false, true),
                true
            ),
            ResizeRight to DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(leftIcon, rightIcon),
                booleanArrayOf(true, false),
                false
            ),
            ResizeLeft to DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(leftIcon, rightIcon),
                booleanArrayOf(false, true),
                false
            )
        )
    }

    /** The set of icons currently shown. */
    private var iconsShown: MutableSet<GestureType> = HashSet()
    /** The current area of the selector. */
    private val selectorArea = Rect()
    /** The current alpha value to applied to all currently shown icons. */
    var alpha: Int = 0
        set(value) {
            field = value
            hintsIcons.forEach {
                it.value.setAlpha(value)
            }
            invalidate()
        }

    /**
     * Show the hint for the provided gesture.
     *
     * @param gesture the gesture to show the hint of.
     */
    fun show(gesture: GestureType) {
        iconsShown = mutableSetOf(gesture)
        alpha = 255
        setSelectorArea(selectorArea)
    }

    /** Show all hints. */
    fun showAll() {
        iconsShown = hintsIcons.keys.toMutableSet()
        alpha = 255
        setSelectorArea(selectorArea)
    }

    /**
     * Invalidate all the hints and update their positions.
     * If the view is visible, [onDraw] will be called at some point in the future.
     *
     * @param newSelectorArea the current selector area.
     */
    fun setSelectorArea(newSelectorArea: Rect) {
        selectorArea.set(newSelectorArea)

        val allShown = iconsShown.size == hintsIcons.size
        hintsIcons.forEach { (hintType, hint) ->
            when(hintType) {
                MoveSelector -> hint.invalidate(newSelectorArea, newSelectorArea.centerX(),
                    newSelectorArea.centerY())
                ZoomCapture -> hint.invalidate(newSelectorArea, newSelectorArea.centerX(),
                    newSelectorArea.top - pinchIconMargin)
                ResizeTop -> hint.invalidate(newSelectorArea, newSelectorArea.centerX(),
                    newSelectorArea.top, booleanArrayOf(false, allShown))
                ResizeBottom -> hint.invalidate(newSelectorArea, newSelectorArea.centerX(),
                    newSelectorArea.bottom, booleanArrayOf(allShown, false))
                ResizeLeft -> hint.invalidate(newSelectorArea, newSelectorArea.left,
                    newSelectorArea.centerY(), booleanArrayOf(false, allShown))
                ResizeRight -> hint.invalidate(newSelectorArea, newSelectorArea.right,
                    newSelectorArea.centerY(), booleanArrayOf(allShown, false))
            }
        }
        invalidate()
    }

    override fun onViewSizeChanged(w: Int, h: Int) {
        super.onViewSizeChanged(w, h)
        maxRect.set(maxArea.toRect())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    override fun onDraw(canvas: Canvas) {
        iconsShown.forEach { hintType ->
            hintsIcons[hintType]!!.draw(canvas)
        }
    }

    override fun onReset() {
        alpha = 0
        iconsShown.clear()
    }
}

/** The default distance between the hint and the selector border. */
private const val DEFAULT_HINTS_ICON_MARGIN = 5
/** The default size of the icons for the hints. */
private const val DEFAULT_HINTS_ICON_SIZE = 10
/** The default margin between the top of the selector and the pinch hint. */
private const val DEFAULT_HINTS_PINCH_ICON_MARGIN = 100
