/*
 * Copyright (C) 2025 Kevin Buzeau
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

package com.buzbuz.smartautoclicker.core.ui.views.zoomedView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.withClip

/** A view that displays a zoomed-in portion of an image centered around a specific pixel. */
class ZoomedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /** Matrix used to calculate the zoom and translation. */
    private val zoomMatrix = Matrix()
    /** Inverse matrix used for mapping touch coordinates to bitmap coordinates. */
    private val inverseMatrix = Matrix()
    /** Float array used for touch coordinate mapping. */
    private val touchPoint = FloatArray(2)

    /** The X coordinate in the source bitmap to center the zoom on. */
    private var zoomCenterX = 0f
    /** The Y coordinate in the source bitmap to center the zoom on. */
    private var zoomCenterY = 0f

    /** Bitmap currently set. */
    private var currentBitmap: Bitmap? = null

    /** Callback for when the zoom position is changed by the user clicking on the view. */
    var onPixelSelected: ((Float, Float) -> Unit)? = null

    /** Paint for the white part of the selector rectangle. */
    private val whitePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    /** Paint for the black part of the selector rectangle. */
    private val blackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    /** Paint for the white part of the grid lines. */
    private val gridWhitePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    /** Paint for the black part of the grid lines. */
    private val gridBlackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    /** The rectangle for the selector. */
    private val selectorRect = RectF()
    /** The rectangle for clipping the grid. */
    private val gridClipRect = RectF()
    /** The grid lines to be drawn. */
    private var gridLines: FloatArray? = null

    init {
        // Use matrix scale type to manually control the zoom and translation.
        scaleType = ScaleType.MATRIX
    }

    /**
     * Sets the center position of the zoom in bitmap pixel coordinates.
     *
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     */
    fun setZoomPosition(x: Float, y: Float) {
        zoomCenterX = x
        zoomCenterY = y
        updateMatrix()
    }

    /**
     * Sets the center position of the zoom in bitmap pixel coordinates.
     *
     * @param position The position in pixels.
     */
    fun setZoomPosition(position: PointF) {
        setZoomPosition(position.x, position.y)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        updateMatrix()
        updateGridLines()
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        if (currentBitmap == bitmap) return
        currentBitmap = bitmap

        super.setImageBitmap(bitmap)

        updateMatrix()
        updateGridLines()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val bitmap = currentBitmap
        if (!isEnabled || bitmap == null) return super.onTouchEvent(event)

        if (event.actionMasked != MotionEvent.ACTION_UP) return true

        // Map view coordinates to bitmap coordinates.
        // ImageView applies padding before applying the matrix, so we subtract it here.
        imageMatrix.invert(inverseMatrix)
        touchPoint[0] = event.x - paddingLeft
        touchPoint[1] = event.y - paddingTop
        inverseMatrix.mapPoints(touchPoint)

        // Constrain the coordinates to the bitmap bounds and snap to the pixel.
        onPixelSelected?.invoke(
            touchPoint[0].coerceIn(0f, bitmap.width.toFloat() - 1).toInt().toFloat(),
            touchPoint[1].coerceIn(0f, bitmap.height.toFloat() - 1).toInt().toFloat(),
        )
        performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val lines = gridLines ?: return
        if (width == 0 || height == 0) return

        // Pixel grid
        canvas.withClip(gridClipRect) {
            drawLines(lines, gridWhitePaint)
            drawLines(lines, gridBlackPaint)
        }

        // Central selector
        canvas.drawRect(selectorRect, blackPaint)
        canvas.drawRect(selectorRect, whitePaint)
    }

    override fun invalidate() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth > 0f && viewHeight > 0f) {
            val scale = viewWidth * ZOOM_FACTOR

            // The pixel is centered in the view (ignoring padding for the center point).
            val left = (viewWidth - scale) / 2f
            val top = (viewHeight - scale) / 2f
            val right = (viewWidth + scale) / 2f
            val bottom = (viewHeight + scale) / 2f

            // Drawing the rectangle around the pixel.
            val offset = blackPaint.strokeWidth / 2f
            selectorRect.set(left - offset, top - offset, right + offset, bottom + offset)
        }
        super.invalidate()
    }

    /** Updates the grid lines to be drawn on the view. */
    private fun updateGridLines() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val scale = w * ZOOM_FACTOR
        val centerX = w / 2f
        val centerY = h / 2f

        val lines = mutableListOf<Float>()

        // Vertical lines
        var x = centerX - 0.5f * scale
        while (x > -scale) {
            lines.add(x); lines.add(0f); lines.add(x); lines.add(h)
            x -= scale
        }
        x = centerX + 0.5f * scale
        while (x < w + scale) {
            lines.add(x); lines.add(0f); lines.add(x); lines.add(h)
            x += scale
        }

        // Horizontal lines
        var y = centerY - 0.5f * scale
        while (y > -scale) {
            lines.add(0f); lines.add(y); lines.add(w); lines.add(y)
            y -= scale
        }
        y = centerY + 0.5f * scale
        while (y < h + scale) {
            lines.add(0f); lines.add(y); lines.add(w); lines.add(y)
            y += scale
        }

        gridLines = lines.toFloatArray()
    }

    /** Updates the image matrix based on the current zoom center and view dimensions. */
    private fun updateMatrix() {
        val d = drawable ?: return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth == 0f || viewHeight == 0f) return

        // Disable filtering to ensure sharp pixels.
        (d as? BitmapDrawable)?.isFilterBitmap = false

        val scale = viewWidth * ZOOM_FACTOR

        zoomMatrix.reset()
        // 1. Scale the bitmap.
        zoomMatrix.postScale(scale, scale)

        // 2. Translate so that (zoomCenterX, zoomCenterY) in the source ends up at the center of the view.
        // We must account for the fact that ImageView translates the canvas by padding before applying the matrix.
        val targetX = (viewWidth / 2f) - paddingLeft
        val targetY = (viewHeight / 2f) - paddingTop
        val dx = targetX - ((zoomCenterX + 0.5f) * scale)
        val dy = targetY - ((zoomCenterY + 0.5f) * scale)
        zoomMatrix.postTranslate(dx, dy)

        // Calculate and cache the clip rectangle for the grid.
        // The clip rect is in the view's coordinate system (accounting for padding).
        val dxOnCanvas = dx + paddingLeft
        val dyOnCanvas = dy + paddingTop
        gridClipRect.set(dxOnCanvas, dyOnCanvas, dxOnCanvas + d.intrinsicWidth * scale, dyOnCanvas + d.intrinsicHeight * scale)

        // Apply the matrix to the ImageView.
        imageMatrix = zoomMatrix
    }

    companion object {
        /** The zoom factor: one source pixel takes ZOOM_FACTOR of the view's width. */
        private const val ZOOM_FACTOR = 0.1f
    }
}
