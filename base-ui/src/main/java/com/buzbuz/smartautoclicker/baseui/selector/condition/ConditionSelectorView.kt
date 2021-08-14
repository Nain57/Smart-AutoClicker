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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.buzbuz.smartautoclicker.extensions.ScreenMetrics
import com.buzbuz.smartautoclicker.ui.R

/**
 * Overlay view used as screenOverlayView showing the area to capture the content as a click condition.
 * This view allows to zoom/move the bitmap displayed as background, as well as display a selector over it allowing to
 * easily select a section of the screen for a click condition.
 *
 * @param context the Android context
 * @param screenMetrics the current screen metrics.
 */
@SuppressLint("ViewConstructor") // Not intended to be used from XML
class ConditionSelectorView(
    context: Context,
    private val screenMetrics: ScreenMetrics
) : View(context) {

    /** */
    private val selector = Selector(context, screenMetrics, ::invalidate)
    /** */
    private val capture = Capture(context, screenMetrics, ::invalidate)
    /** Controls the display of the user hints around the selector. */
    private lateinit var hintsIcons: SelectorHintsController
    /**
     *
     */
    private lateinit var animations: Animations

    /** Paint drawing the selector. */
    private val selectorPaint = Paint()
    /** Paint for the background of the selector. */
    private val backgroundPaint = Paint()
    /** The radius of the corner for the selector. */
    private var cornerRadius = 0f

    /** The drawable for the screen capture. */
    private var screenCapture: BitmapDrawable? = null

    /** Tell if the content of this view should be hidden or not. */
    var hide = true
        set(value) {
            if (field == value) {
                return
            }

            field = value
            invalidate()
        }

    init {
        var hintIconsSize = 10
        var hintIconsMargin = 5
        @ColorInt var outlineColor = Color.WHITE
        var hintFadeDuration = 500
        var hintAllFadeDelay = 1000

        context.obtainStyledAttributes(R.style.OverlaySelectorView_Condition, R.styleable.ConditionSelectorView).use { ta ->
            hintIconsSize = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_hintsIconsSize, hintIconsSize)
            hintIconsMargin = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_hintsIconsMargin, hintIconsMargin)
            outlineColor =  ta.getColor(R.styleable.ConditionSelectorView_colorOutlinePrimary, outlineColor)
            hintFadeDuration = ta.getInteger(R.styleable.ConditionSelectorView_hintsFadeDuration, hintFadeDuration)
            hintAllFadeDelay = ta.getInteger(R.styleable.ConditionSelectorView_hintsAllFadeDelay, hintAllFadeDelay)

            cornerRadius = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_cornerRadius, 2)
                .toFloat()

            val thickness = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_thickness, 4).toFloat()
            selectorPaint.apply {
                style = Paint.Style.STROKE
                strokeWidth = thickness
                color = outlineColor
                alpha = 0
            }
            backgroundPaint.apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = ta.getColor(R.styleable.ConditionSelectorView_colorBackground, Color.TRANSPARENT)
            }

            selector.setDefaultValues(
                defaultWidth =  ta.getDimensionPixelSize(
                    R.styleable.ConditionSelectorView_defaultWidth,
                    100
                ).toFloat() / 2,
                defaultHeight = ta.getDimensionPixelSize(
                    R.styleable.ConditionSelectorView_defaultHeight,
                    100
                ).toFloat() / 2,
                handle = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_resizeHandleSize, 10)
                    .toFloat(),
                areaOffset = kotlin.math.ceil(thickness / 2).toInt()
            )

            animations = Animations(ta)
            hintsIcons = SelectorHintsController(context, hintIconsSize, screenMetrics, hintIconsMargin,
                outlineColor, hintFadeDuration.toLong(), hintAllFadeDelay.toLong(), this)
        }
    }

    /** */
    init {
        animations.apply {
            onCaptureZoomLevelChanged = capture::setZoomLevel
            onSelectorBorderAlphaChanged = { alpha ->
                selectorPaint.alpha = alpha
                invalidate()
            }
            onSelectorBackgroundAlphaChanged = { alpha ->
                backgroundPaint.alpha = alpha
                invalidate()
            }
            onHintsAlphaChanged = { alpha ->
                hintsIcons.alpha = alpha
                hintsIcons.invalidate()
            }
        }

        selector.onSelectorPositionChanged = { position ->
            hintsIcons.invalidate(position)
        }
    }

    /**
     * Shows the capture on the screen.
     *
     * @param bitmap the capture the be shown.
     */
    fun showCapture(bitmap: Bitmap) {
        screenCapture = BitmapDrawable(resources, bitmap)

        hintsIcons.showAll()
        animations.startShowSelectorAnimation(
            onAnimationCompleted = {
                animations.startHideHintsAnimation()
            }
        )
    }

    /**
     * Get the part of the capture that is currently selected within the selector.
     *
     * @return a pair of the capture area and a bitmap of its content.
     */
    fun getSelection(): Pair<Rect, Bitmap> {
        val selectionArea = selector.getSelectionArea(capture.captureArea, capture.zoomLevel)
        return selectionArea to Bitmap.createBitmap(screenCapture!!.bitmap, selectionArea.left,
            selectionArea.top, selectionArea.width(), selectionArea.height())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        selector.onViewSizeChanged(w, h)
        capture.onViewSizeChanged(w, h)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        selector.currentGesture?.let { gestureType ->
            hintsIcons.show(gestureType)
            animations.cancelHideHintsAnimation()

            if (event.action == ACTION_UP) {
                animations.startHideHintsAnimation()
            }
        }

        return selector.onTouchEvent(event) || capture.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        if (hide) {
            return
        }

        screenCapture?.apply {
            canvas.drawColor(Color.BLACK)
            setBounds(
                capture.captureArea.left.toInt(),
                capture.captureArea.top.toInt(),
                capture.captureArea.right.toInt(),
                capture.captureArea.bottom.toInt()
            )
            draw(canvas)
        }

        canvas.drawRoundRect(selector.selectorArea, cornerRadius, cornerRadius, selectorPaint)
        canvas.drawRect(selector.selectedArea, backgroundPaint)

        hintsIcons.onDraw(canvas)
    }
}