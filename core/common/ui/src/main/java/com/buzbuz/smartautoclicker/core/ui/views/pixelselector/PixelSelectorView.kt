/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.pixelselector

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.content.res.use
import androidx.core.graphics.drawable.toDrawable

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.CaptureComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.PixelPositionComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ComponentsView
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor") // Not intended to be used from XML
class PixelSelectorView(
    context: Context,
    private val displayConfigManager: DisplayConfigManager,
    private val onSelectedPositionChanged: (PointF) -> Unit,
) : ComponentsView(context)  {

    /** The bitmap currently set to the view. */
    private var currentBitmap: Bitmap? = null
    /** Controls the display of the bitmap captured. */
    private lateinit var capture: CaptureComponent
    /** Controls the display the currently selected pixel. */
    private lateinit var position: PixelPositionComponent

    /** Get the attributes from the style file and initialize all components. */
    init {
        context.obtainPixelSelectorViewStyledAttributes().use { ta ->
            capture = CaptureComponent(context, ta.getCaptureComponentStyle(displayConfigManager), this)
            position = PixelPositionComponent(ta.getPixelPositionComponentStyle(displayConfigManager), this)
        }
    }

    override val viewComponents: List<ViewComponent> = listOf(capture, position)


    fun updateCapture(bitmap: Bitmap?) {
        if (bitmap == currentBitmap) return
        currentBitmap = bitmap

        capture.screenCapture = bitmap?.toDrawable(resources)
        invalidate()
    }

    fun updatePixelPosition(x: Float = position.pixelPosition.x, y: Float = position.pixelPosition.y) {
        if (position.pixelPosition.equals(x, y)) return
        position.pixelPosition.set(x.roundToInt().toFloat(), y.roundToInt().toFloat())
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        position.pixelPosition.set(event.x.roundToInt().toFloat(), event.y.roundToInt().toFloat())
        onSelectedPositionChanged(PointF(position.pixelPosition.x, position.pixelPosition.y))
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    private fun Context.obtainPixelSelectorViewStyledAttributes(): TypedArray =
        obtainStyledAttributes(null, R.styleable.PixelSelectorView, R.attr.pixelSelectorStyle, 0)
}