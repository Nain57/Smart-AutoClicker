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
package com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent

import androidx.annotation.DrawableRes
import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.GestureType
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.MoveSelector
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeBottom
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeLeft
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeRight
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeTop
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ZoomCapture
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewStyle

/**
 * Controls the hints and their animations state.
 *
 * @param context the Android Context.
 * @param hintsStyle the style for the hints.
 * @param viewInvalidator calls invalidate on the view hosting this component.
 */
internal class HintsComponent(
    context: Context,
    hintsStyle: HintsComponentStyle,
    viewInvalidator: () -> Unit,
): ViewComponent(hintsStyle, viewInvalidator) {

    /** The distance between the hint and the selector border. */
    private val iconsMargin: Int = hintsStyle.iconsMargin
    /** The size of the icons for the hints. */
    private val iconsSize: Int = hintsStyle.iconsSize
    /** The margin between the top of the selector and the pinch hint. */
    private val pinchIconMargin: Int = hintsStyle.pinchIconMargin ?: 0

    /** Map between a gesture type and its hint. */
    private val hintsIcons: Map<GestureType, Hint>
    /** Maximum area for the hints. */
    private val maxRect = Rect()

    /** Initialize the list of hints. */
    init {
        maxRect.set(maxArea.toRect())
        hintsIcons = buildMap {
            put(MoveSelector, SingleHint(
                context,
                iconsSize,
                maxRect,
                hintsStyle.moveIcon,
                booleanArrayOf(true)
            ))
            if (hintsStyle.pinchIcon != null) {
                put(ZoomCapture, SingleHint(
                    context,
                    iconsSize,
                    maxRect,
                    hintsStyle.pinchIcon,
                    booleanArrayOf(false)
                ))
            }
            put(ResizeBottom, DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(hintsStyle.upIcon, hintsStyle.downIcon),
                booleanArrayOf(true, false),
                true
            ))
            put(ResizeTop, DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(hintsStyle.upIcon, hintsStyle.downIcon),
                booleanArrayOf(false, true),
                true
            ))
            put(ResizeRight, DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(hintsStyle.leftIcon, hintsStyle.rightIcon),
                booleanArrayOf(true, false),
                false
            ))
            put(ResizeLeft, DoubleHint(
                context,
                iconsSize,
                maxRect,
                iconsMargin,
                intArrayOf(hintsStyle.leftIcon, hintsStyle.rightIcon),
                booleanArrayOf(false, true),
                false
            ))
        }
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

/**
 * Style for [HintsComponent].
 *
 * @param displayMetrics metrics for the device display.
 * @param iconsMargin the distance between the hint and the selector border.
 * @param iconsSize the size of the icons for the hints.
 * @param moveIcon icon res for the moving hint.
 * @param upIcon icon res for the resize up hint.
 * @param downIcon icon res for the resize down hint.
 * @param leftIcon icon res for the resize left hint.
 * @param rightIcon icon res for the resize right hint.
 * @param pinchIcon icon res for the resize pinch hint.
 * @param pinchIconMargin the margin between the top of the selector and the pinch hint.
 *
 */
internal class HintsComponentStyle(
    displayMetrics: DisplayMetrics,
    val iconsMargin: Int,
    val iconsSize: Int,
    @DrawableRes val moveIcon: Int,
    @DrawableRes val upIcon: Int,
    @DrawableRes val downIcon: Int,
    @DrawableRes val leftIcon: Int,
    @DrawableRes val rightIcon: Int,
    @DrawableRes val pinchIcon: Int? = null,
    val pinchIconMargin: Int? = null,
) : ViewStyle(displayMetrics)

/** The default distance between the hint and the selector border. */
internal const val DEFAULT_HINTS_ICON_MARGIN = 5
/** The default size of the icons for the hints. */
internal const val DEFAULT_HINTS_ICON_SIZE = 10
/** The default margin between the top of the selector and the pinch hint. */
internal const val DEFAULT_HINTS_PINCH_ICON_MARGIN = 100
