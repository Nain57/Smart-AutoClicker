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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable

import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.R

/** Factory object for creating a [Hint]. */
object HintFactory {

    /** Size of the hint icons. */
    var iconsSize = 0
    /** The area to draw in. Any hint outside this area will be hidden. */
    var maxArea = Rect()
    /** The distance between two icons in a Hint. */
    var iconsMargin = 0

    /**
     * Build a single Hint.
     *
     * @param context The Android context.
     * @param iconId The identifier of the drawable resource containing the hint icon.
     * @param inSelector true if this hint is inside the selector area and should be hidden if it is too narrow, false
     *                   to hide on maxArea instead.
     *
     * @return the new Hint.
     */
    fun buildHint(context: Context, @DrawableRes iconId: Int, inSelector: Boolean): Hint {
        return SingleHint(context, iconsSize, maxArea, iconId, inSelector)
    }

    /**
     * Build a double Hint.
     *
     * @param context The Android context.
     * @param icons The identifiers of the two drawable resources containing the hint icons.
     * @param inSelector true if this hint is inside the selector area and should be hidden if it is too narrow, false
     *                   to hide on maxArea instead. Index correspond with icons param.
     * @param isVertical true if the two icons are placed vertically, false if horizontally.
     *
     * @return the new Hint.
     */
    fun buildHint(context: Context, @DrawableRes @Size(2) icons: IntArray,
                  @Size(2)  inSelector: BooleanArray, isVertical: Boolean,): Hint {
        return DoubleHint(context, iconsSize, maxArea, iconsMargin, icons, inSelector, isVertical)
    }
}

/**
 * Base class for a Hint.
 * A hint is a small indicative icon showing to the user the possible actions to execute on a overlay view (such as
 * move, resize ...)
 *
 * @param iconsSize the size of the hint icons.
 * @param maxArea area to draw in. Any icon outside this area will be hidden.
 */
abstract class Hint(iconsSize: Int, protected val maxArea: Rect) {

    /** The half of an icon size. */
    protected val iconHalfSize = iconsSize / 2

    /**
     * Invalidate the drawing values of the hint and refresh them for the drawing pass.
     *
     * @param selectorArea the area of the overlay view.
     * @param newCenterX the new center x position of the hint.
     * @param newCenterY the new center y position of the hint.
     * @param alpha the new alpha of the hint icon.
     * @param hide the icons to hide.
     */
    abstract fun invalidate(selectorArea: Rect, newCenterX: Int, newCenterY: Int, alpha: Int,
                            hide: BooleanArray? = booleanArrayOf(false))

    /**
     * Draw the Hint into the provided canvas.
     *
     * @param canvas the canvas to draw the icon on.
     */
    abstract fun draw(canvas: Canvas)

    /**
     * Get the mutable version of the provided drawable resource identifier.
     *
     * @param context the Android context used for drawable inflation.
     * @param id the resource identifier for the drawable
     *
     * @return the mutable drawable.
     */
    protected fun getMutableDrawable(context: Context, @DrawableRes id: Int) : Drawable =
        ContextCompat.getDrawable(context, id)!!.apply {
            setTint(context.resources.getColor(R.color.overlayViewPrimary, null))
        }.mutate()
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
private open class SingleHint(
    context: Context,
    iconsSize: Int,
    maxArea: Rect,
    @DrawableRes iconId: Int,
    private val inSelector: Boolean
) : Hint(iconsSize, maxArea) {

    /** The drawable of the icon. */
    private val icon = getMutableDrawable(context, iconId)

    override fun invalidate(selectorArea: Rect, newCenterX: Int, newCenterY: Int, alpha: Int,
                            @Size(1) hide: BooleanArray?) {
        invalidate(selectorArea, newCenterX, newCenterY, alpha, hide!![0])
    }

    /**
     * Invalidate the drawing values of the hint and refresh them for the drawing pass.
     *
     * @param selectorArea the area of the overlay view.
     * @param newCenterX the new center x position of the hint.
     * @param newCenterY the new center y position of the hint.
     * @param alpha the new alpha of the hint icon.
     * @param hide true to hide the icon, false to displays it, if possible.
     */
    open fun invalidate(selectorArea: Rect, newCenterX: Int, newCenterY: Int, alpha: Int, hide: Boolean) {
        if (hide) {
            icon.alpha = 0
            return
        }

        icon.bounds = Rect(
            newCenterX - iconHalfSize,
            newCenterY - iconHalfSize,
            newCenterX + iconHalfSize,
            newCenterY + iconHalfSize
        )

        icon.alpha = if (inSelector && selectorArea.contains(icon.bounds) ||
            !inSelector && maxArea.contains(icon.bounds)) alpha else 0
    }

    override fun draw(canvas: Canvas) {
        icon.draw(canvas)
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
private class DoubleHint(
    context: Context,
    iconsSize: Int,
    maxArea: Rect,
    private val iconsMargin: Int,
    @DrawableRes @Size(2) icons: IntArray,
    @Size(2) private val inSelector: BooleanArray,
    private val isVertical: Boolean,
): SingleHint(context, iconsSize, maxArea, icons[0], inSelector[0]) {

    /** The second icon for this hint. */
    private val secondHint = SingleHint(context, iconsSize, maxArea, icons[1], inSelector[1])

    override fun invalidate(selectorArea: Rect, newCenterX: Int, newCenterY: Int, alpha: Int,
                            @Size(2) hide: BooleanArray?) {
        if (isVertical) {
            super.invalidate(selectorArea, newCenterX, newCenterY - iconsMargin - iconHalfSize, alpha, hide!![0])
            secondHint.invalidate(selectorArea, newCenterX, newCenterY + iconsMargin + iconHalfSize, alpha, hide[1])
        } else {
            super.invalidate(selectorArea, newCenterX - iconsMargin - iconHalfSize, newCenterY, alpha, hide!![0])
            secondHint.invalidate(selectorArea, newCenterX + iconsMargin + iconHalfSize, newCenterY, alpha, hide[1])
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        secondHint.draw(canvas)
    }
}