/*
 * Copyright (C) 2024 Kevin Buzeau
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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View

import androidx.core.view.children
import androidx.core.content.res.use
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

import kotlin.math.roundToInt

class ConditionalDividerItemDecoration(
    context: Context,
    initialOrientation: Int,
    private val excludedViewTypes: Set<Int>,
) : ItemDecoration() {

    private val bounds: Rect = Rect()

    var divider: Drawable? =
        context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider)).use { ta ->
            ta.getDrawable(0) ?: let {
                Log.w(TAG, "@android:attr/listDivider was not set in the theme used for this "
                        + "DividerItemDecoration. Please set that attribute all call setDrawable()")
                null
            }
        }

    var orientation: Int = initialOrientation
        set(value) {
            require(value == DividerItemDecoration.HORIZONTAL
                    || value == DividerItemDecoration.VERTICAL) {
                "Invalid orientation. It should be either HORIZONTAL or VERTICAL"
            }

            field = value
        }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val dividerDrawable = divider
        if (dividerDrawable == null || excludedViewTypes.contains(parent.getViewType(view))) {
            outRect.setEmpty()
            return
        }

        if (orientation == DividerItemDecoration.VERTICAL) {
            outRect.set(0, 0, 0, dividerDrawable.intrinsicHeight)
        } else {
            outRect.set(0, 0, dividerDrawable.intrinsicWidth, 0)
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val drawable = divider
        if (parent.layoutManager == null || drawable == null) return

        if (orientation == DividerItemDecoration.VERTICAL) {
            drawVertical(c, parent, drawable)
        } else {
            drawHorizontal(c, parent, drawable)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView, drawable: Drawable) {
        canvas.save()

        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(left, parent.paddingTop, right, parent.height - parent.paddingBottom)
        } else {
            left = 0
            right = parent.width
        }

        parent.children.forEach { child ->
            if (excludedViewTypes.contains(parent.getViewType(child))) return@forEach

            parent.getDecoratedBoundsWithMargins(child, bounds)
            val bottom: Int = bounds.bottom + child.translationY.roundToInt()
            val top: Int = bottom - drawable.intrinsicHeight
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }

        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView, drawable: Drawable) {
        canvas.save()

        val top: Int
        val bottom: Int
        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(parent.paddingLeft, top, parent.width - parent.paddingRight, bottom)
        } else {
            top = 0
            bottom = parent.height
        }

        parent.children.forEach { child ->
            if (excludedViewTypes.contains(parent.getViewType(child))) return@forEach

            parent.layoutManager?.getDecoratedBoundsWithMargins(child, bounds)
            val right: Int = bounds.right + child.translationX.roundToInt()
            val left: Int = right - drawable.intrinsicWidth
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }

        canvas.restore()
    }
}

private fun RecyclerView.getViewType(view: View): Int? {
    val position = getChildAdapterPosition(view)
    if (position == RecyclerView.NO_POSITION) return null

    return adapter?.getItemViewType(position)
}

private const val TAG = "ConditionalDividerItemDecoration"