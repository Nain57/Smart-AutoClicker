package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.ScreenConditionResultUiState

/**
 * Displays a rectangle at the selected position to represents the detection.
 * @param context the Android context.
 */
internal class DebugOverlayView(context: Context) : View(context) {

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
    private val conditionBordersMargin = 20

    private val results: MutableList<ScreenConditionResultUiState> = mutableListOf()
    private val displayedResults: MutableList<Pair<Paint, Rect>> = mutableListOf()

    fun setResults(newResults: List<ScreenConditionResultUiState>) {
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

    private fun updateResults(newResults: List<ScreenConditionResultUiState>) {
        if (results != newResults) {
            results.clear()
            results.addAll(newResults)
        }
        displayedResults.clear()

        // No condition matched ? Nothing to display
        if (results.isEmpty()) {
            return
        }

        displayedResults.addAll(results.toDisplayResults())
    }

    private fun List<ScreenConditionResultUiState>.toDisplayResults(): List<Pair<Paint, Rect>> =
        mapNotNull { uiState ->
            if (!uiState.positive && (uiState.coordinates.width() == 0 || uiState.coordinates.height() == 0))
                return@mapNotNull null

            Pair(
                if (uiState.positive) positiveResultPaint else negativeResultPaint,
                Rect(
                    uiState.coordinates.left - conditionBordersMargin,
                    uiState.coordinates.top - conditionBordersMargin,
                    uiState.coordinates.right + conditionBordersMargin,
                    uiState.coordinates.bottom + conditionBordersMargin,
                )
            )
        }

}