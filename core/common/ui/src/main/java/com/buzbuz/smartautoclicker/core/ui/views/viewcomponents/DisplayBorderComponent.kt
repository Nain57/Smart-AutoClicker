
package com.buzbuz.smartautoclicker.core.ui.views.viewcomponents

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.display.config.Corner
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfig
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.config.DisplayRoundedCorner
import com.buzbuz.smartautoclicker.core.display.config.haveRoundedCorner
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewInvalidator
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewStyle


internal class DisplayBorderComponent (
    private val viewStyle: DisplayBorderComponentStyle,
    viewInvalidator: ViewInvalidator,
): ViewComponent(viewStyle, viewInvalidator) {

    private val recordStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = viewStyle.color
        strokeWidth = viewStyle.thicknessPx.toFloat() * 2
    }

    private val recordFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = viewStyle.color
    }

    private val borders: MutableList<Rect> = mutableListOf()
    private val corners: MutableList<Triple<RectF, Float, Float>> = mutableListOf()

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    override fun onReset() {
        borders.clear()
        corners.clear()
    }

    override fun onInvalidate() {
        borders.clear()
        corners.clear()

        val displayConfig: DisplayConfig = viewStyle.displayConfigManager.displayConfig
        if (displayConfig.haveRoundedCorner()) {
            borders.addRoundedDisplayBorderLines(displayConfig)
            corners.addRoundedCorners(displayConfig)
        } else {
            borders.addDisplayRectangleBorder(displayConfig)
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Square screen use case
        if (corners.isEmpty() && borders.size == 1) {
            canvas.drawRect(borders[0], recordStrokePaint)
            return
        }

        borders.forEach { border ->
            canvas.drawRect(border, recordFillPaint)
        }
        corners.forEach { (area, startAngle, sweepAngle) ->
            canvas.drawArc(area, startAngle, sweepAngle, false, recordStrokePaint)
        }
    }

    private fun MutableList<Rect>.addDisplayRectangleBorder(displayConfig: DisplayConfig) {
        add(Rect(0, 0, displayConfig.sizePx.x, displayConfig.sizePx.y))
    }

    private fun MutableList<Rect>.addRoundedDisplayBorderLines(displayConfig: DisplayConfig) {
        // Left
        add(
            Rect(
            0,
            displayConfig.roundedCorners[Corner.TOP_LEFT]?.centerPx?.y ?: 0,
            viewStyle.thicknessPx,
            displayConfig.roundedCorners[Corner.BOTTOM_LEFT]?.centerPx?.y ?: displayConfig.sizePx.y,
        )
        )
        // Top
        add(
            Rect(
            displayConfig.roundedCorners[Corner.TOP_LEFT]?.centerPx?.x ?: 0,
            0,
            displayConfig.roundedCorners[Corner.TOP_RIGHT]?.centerPx?.x ?: displayConfig.sizePx.x,
            viewStyle.thicknessPx,
        )
        )
        // Right
        add(
            Rect(
            displayConfig.sizePx.x - viewStyle.thicknessPx,
            displayConfig.roundedCorners[Corner.TOP_RIGHT]?.centerPx?.y ?: 0,
            displayConfig.sizePx.x,
            displayConfig.roundedCorners[Corner.BOTTOM_RIGHT]?.centerPx?.y ?: displayConfig.sizePx.y,
        )
        )
        // Bottom
        add(
            Rect(
            displayConfig.roundedCorners[Corner.BOTTOM_LEFT]?.centerPx?.x ?: 0,
            displayConfig.sizePx.y - viewStyle.thicknessPx,
            displayConfig.roundedCorners[Corner.BOTTOM_RIGHT]?.centerPx?.x ?: displayConfig.sizePx.y,
            displayConfig.sizePx.y,
        )
        )
    }

    private fun MutableList<Triple<RectF, Float, Float>>.addRoundedCorners(displayConfig: DisplayConfig) {
        val width = displayConfig.sizePx.x.toFloat()
        val height = displayConfig.sizePx.y.toFloat()

        displayConfig.roundedCorners[Corner.TOP_LEFT]?.let { topLeftCorner ->
            add(Triple(
                RectF(0f, 0f, topLeftCorner.getCornerOffsetLeft(), topLeftCorner.getCornerOffsetTop()),
                180f, 90f,
            ))
        }
        displayConfig.roundedCorners[Corner.TOP_RIGHT]?.let { topRightCorner ->
            add(Triple(
                RectF(width - topRightCorner.getCornerOffsetRight(width), 0f,
                    width, topRightCorner.getCornerOffsetTop()),
                270f, 90f,
            ))
        }
        displayConfig.roundedCorners[Corner.BOTTOM_RIGHT]?.let { bottomRightCorner ->
            add(Triple(
                RectF(width - bottomRightCorner.getCornerOffsetRight(width),
                    height - bottomRightCorner.getCornerOffsetBottom(height), width, height),
                0f, 90f,
            ))
        }
        displayConfig.roundedCorners[Corner.BOTTOM_LEFT]?.let { bottomLeftCorner ->
            add(Triple(
                RectF(0f, height - bottomLeftCorner.getCornerOffsetBottom(height),
                    bottomLeftCorner.getCornerOffsetLeft(), height),
                90f, 90f,
            ))
        }
    }

    private fun DisplayRoundedCorner.getCornerOffsetLeft(): Float =
        centerPx.x * 2f + viewStyle.thicknessPx * 4f
    private fun DisplayRoundedCorner.getCornerOffsetTop(): Float =
        centerPx.y * 2f + viewStyle.thicknessPx * 4f
    private fun DisplayRoundedCorner.getCornerOffsetRight(width: Float): Float =
        (width - centerPx.x) * 2f + viewStyle.thicknessPx * 4f
    private fun DisplayRoundedCorner.getCornerOffsetBottom(height: Float): Float =
        (height - centerPx.y) * 2f + viewStyle.thicknessPx * 4f
}

/**
 * Style for [DisplayBorderComponent].
 *
 * @param displayConfigManager metrics for the device display.
 * @param color the color for the border.
 * @param thicknessPx the thickness of the border in pixels.
 */
internal class DisplayBorderComponentStyle(
    displayConfigManager: DisplayConfigManager,
    @ColorInt val color: Int,
    val thicknessPx: Int,
) : ViewStyle(displayConfigManager)