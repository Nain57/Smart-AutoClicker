/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition.selector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect

import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.ui.R

/**
 * Base class for a Hint.
 * A hint is a small indicative icon showing to the user the possible actions to execute on a overlay view (such as
 * move, resize ...)
 *
 * @param iconsSize the size of the hint icons.
 * @param maxArea area to draw in. Any icon outside this area will be hidden.
 */
internal abstract class Hint(
    context: Context,
    iconsSize: Int,
    @DrawableRes iconId: Int,
    private val maxArea: Rect,
    private val inSelector: BooleanArray,
) {

    /** The half of an icon size. */
    protected val iconHalfSize = iconsSize / 2
    /** The drawable of the icon. */
    protected val icon = ContextCompat.getDrawable(context, iconId)!!.apply {
        setTint(context.resources.getColor(R.color.overlayViewPrimary, null))
    }.mutate()
    /** True if this hints should be hidden, false if not. */
    private var isHidden: Boolean = false

    /**
     * Change the transparency of the hint icon.
     *
     * @param alpha the transparency value.
     */
    @CallSuper
    open fun setAlpha(alpha: Int) {
        icon.alpha = alpha
    }

    /**
     * Invalidate the drawing values of the hint and refresh them for the drawing pass.
     *
     * @param selectorArea the area of the overlay view.
     * @param newCenterX the new center x position of the hint.
     * @param newCenterY the new center y position of the hint.
     * @param hideValues the icons to hide.
     */
    @CallSuper
    open fun invalidate(selectorArea: Rect, newCenterX: Int, newCenterY: Int,
                        hideValues: BooleanArray? = booleanArrayOf(false)) {
        val hide = hideValues?.get(0) ?: true
        isHidden = hide || !(inSelector[0] && selectorArea.contains(icon.bounds) || !inSelector[0] && maxArea.contains(icon.bounds))
    }

    /**
     * Draw the Hint into the provided canvas.
     *
     * @param canvas the canvas to draw the icon on.
     */
    @CallSuper
    open fun draw(canvas: Canvas) {
        if (isHidden) return
        icon.draw(canvas)
    }
}

/**
 * A hint with one icon, centered on the position.
 *
 * @param context The Android context.
 * @param iconsSize The size of the hint icons.
 * @param maxArea area to draw in. Any icon outside this area will be hidden.
 * @param iconId the resource identifier for the hint icon.
 * @param inSelector true if the bounds limits for this hint is the selector, false for the max area.
 */
internal open class SingleHint(
    context: Context,
    iconsSize: Int,
    maxArea: Rect,
    @DrawableRes iconId: Int,
    @Size(1) private val inSelector: BooleanArray,
) : Hint(context, iconsSize, iconId, maxArea, inSelector) {

    override fun invalidate(selectorArea: Rect, newCenterX: Int, newCenterY: Int,
                            @Size(1) hideValues: BooleanArray?) {
        icon.bounds = Rect(
            newCenterX - iconHalfSize,
            newCenterY - iconHalfSize,
            newCenterX + iconHalfSize,
            newCenterY + iconHalfSize
        )
        super.invalidate(selectorArea, newCenterX, newCenterY, hideValues)
    }
}

/**
 * A hint with two icons, spaced by a margin, with a pivot on the position.
 *
 * @param context The Android context.
 * @param iconsSize The size of the hint icons.
 * @param iconsMargin the space between the icons and the position.
 * @param icons the resources identifier for the hint icons.
 * @param inSelector true if the bounds limits for this hint is the selector, false for the max area. Index correspond
 *                   with icons param.
 * @param isVertical true if the two icons are placed vertically, false if horizontally.
 */
internal class DoubleHint(
    context: Context,
    iconsSize: Int,
    maxArea: Rect,
    private val iconsMargin: Int,
    @DrawableRes @Size(2) icons: IntArray,
    @Size(2) private val inSelector: BooleanArray,
    private val isVertical: Boolean,
): SingleHint(context, iconsSize, maxArea, icons[0], booleanArrayOf(inSelector[0])) {

    /** The second icon for this hint. */
    private val secondHint = SingleHint(context, iconsSize, maxArea, icons[1], booleanArrayOf(inSelector[1]))

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)
        secondHint.setAlpha(alpha)
    }

    override fun invalidate(selectorArea: Rect, newCenterX: Int, newCenterY: Int,
                            @Size(2) hideValues: BooleanArray?) {
        if (isVertical) {
            super.invalidate(selectorArea, newCenterX, newCenterY - iconsMargin - iconHalfSize,
                booleanArrayOf(hideValues!![0]))
            secondHint.invalidate(selectorArea, newCenterX, newCenterY + iconsMargin + iconHalfSize,
                booleanArrayOf(hideValues[1]))
        } else {
            super.invalidate(selectorArea, newCenterX - iconsMargin - iconHalfSize, newCenterY,
                booleanArrayOf(hideValues!![0]))
            secondHint.invalidate(selectorArea, newCenterX + iconsMargin + iconHalfSize, newCenterY,
                booleanArrayOf(hideValues[1]))
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        secondHint.draw(canvas)
    }
}