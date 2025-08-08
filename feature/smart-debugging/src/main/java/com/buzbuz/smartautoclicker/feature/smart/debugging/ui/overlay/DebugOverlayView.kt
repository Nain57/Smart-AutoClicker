
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

/**
 * Displays a rectangle at the selected position to represents the detection.
 * @param context the Android context.
 */
class DebugOverlayView(context: Context) : View(context) {

    private val positiveResultPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val negativeResultPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    /** The margin between the actual condition position and the displayed borders. */
    private val conditionBordersMargin = 10

    private val results: MutableList<DetectionResultInfo> = mutableListOf()
    private val displayedResults: MutableList<Pair<Paint, Rect>> = mutableListOf()

    fun setResults(newResults: List<DetectionResultInfo>) {
        updateResults(newResults)
        postInvalidate()
    }

    fun clear() {
        updateResults(emptyList())
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateResults(results)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        displayedResults.forEach { (paint, coordinates) ->
            canvas.drawRect(coordinates, paint)
        }
    }

    private fun updateResults(newResults: List<DetectionResultInfo>) {
        if (results != newResults) {
            results.clear()
            results.addAll(newResults)
        }
        displayedResults.clear()

        // No condition matched ? Nothing to display
        if (results.isEmpty()) {
            return
        }

        results.forEach { result -> displayedResults.add(result.toDisplayResult()) }
    }

    private fun DetectionResultInfo.toDisplayResult(): Pair<Paint, Rect> = Pair(
        if (positive) positiveResultPaint else negativeResultPaint,
        Rect(
            coordinates.left - conditionBordersMargin,
            coordinates.top - conditionBordersMargin,
            coordinates.right + conditionBordersMargin,
            coordinates.bottom + conditionBordersMargin,
        )
    )
}

data class DetectionResultInfo(
    val positive: Boolean,
    val coordinates: Rect,
    val confidenceRate: Double,
)