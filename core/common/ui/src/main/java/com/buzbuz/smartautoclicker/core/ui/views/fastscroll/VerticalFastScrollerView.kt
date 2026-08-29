/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.ui.views.fastscroll

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

/** A draggable vertical scrollbar with a usable minimum thumb and touch target. */
class VerticalFastScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val idleThumbWidth = 8f * density
    private val draggingThumbWidth = 14f * density
    private val idleTrackWidth = 3f * density
    private val draggingTrackWidth = 8f * density
    private val minimumThumbHeight = 48f * density
    private val verticalMargin = 4f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbBounds = RectF()
    private var thumbColor: Int = 0
    private var dragVisualProgress = 0f
    private var dragVisualAnimator: ValueAnimator? = null

    private var recyclerView: RecyclerView? = null
    private var listenerAttached = false
    private var dragging = false
    private var dragOffsetY = 0f

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateThumbBounds()
        }
    }

    init {
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.colorAccent)) {
            thumbColor = getColor(0, 0)
        }
        updatePaintColors()
        isClickable = true
    }

    fun attachToRecyclerView(view: RecyclerView) {
        if (recyclerView === view) return
        detachScrollListener()
        recyclerView = view
        attachScrollListener()
        view.post(::updateThumbBounds)
    }

    fun refresh() {
        post(::updateThumbBounds)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachScrollListener()
        refresh()
    }

    override fun onDetachedFromWindow() {
        dragVisualAnimator?.cancel()
        detachScrollListener()
        super.onDetachedFromWindow()
    }

    private fun attachScrollListener() {
        if (listenerAttached) return
        recyclerView?.addOnScrollListener(scrollListener)
        listenerAttached = recyclerView != null
    }

    private fun detachScrollListener() {
        if (!listenerAttached) return
        recyclerView?.removeOnScrollListener(scrollListener)
        listenerAttached = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (thumbBounds.isEmpty) return

        val thumbWidth = lerp(idleThumbWidth, draggingThumbWidth, dragVisualProgress)
        val trackWidth = lerp(idleTrackWidth, draggingTrackWidth, dragVisualProgress)

        val thumbLeft = edgeAlignedLeft(thumbWidth)
        val trackLeft = edgeAlignedLeft(trackWidth)
        canvas.drawRoundRect(
            trackLeft,
            verticalMargin,
            trackLeft + trackWidth,
            height - verticalMargin,
            trackWidth / 2f,
            trackWidth / 2f,
            trackPaint,
        )
        canvas.drawRoundRect(
            thumbLeft,
            thumbBounds.top,
            thumbLeft + thumbWidth,
            thumbBounds.bottom,
            thumbWidth / 2f,
            thumbWidth / 2f,
            thumbPaint,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (thumbBounds.isEmpty) return false

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val touchTop = thumbBounds.centerY() - max(minimumThumbHeight, thumbBounds.height()) / 2f
                val touchBottom = thumbBounds.centerY() + max(minimumThumbHeight, thumbBounds.height()) / 2f
                if (event.y !in touchTop..touchBottom) return false

                parent.requestDisallowInterceptTouchEvent(true)
                dragging = true
                dragOffsetY = event.y - thumbBounds.top
                isPressed = true
                animateDraggingAppearance(show = true)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                scrollToThumbTop(event.y - dragOffsetY)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                isPressed = false
                animateDraggingAppearance(show = false)
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                true
            }
            else -> dragging
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun animateDraggingAppearance(show: Boolean) {
        dragVisualAnimator?.cancel()
        dragVisualAnimator = ValueAnimator.ofFloat(dragVisualProgress, if (show) 1f else 0f).apply {
            duration = 140L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                dragVisualProgress = animator.animatedValue as Float
                updatePaintColors()
                invalidate()
            }
            start()
        }
    }

    private fun updatePaintColors() {
        val thumbAlpha = lerp(205f, 255f, dragVisualProgress).toInt()
        val trackAlpha = lerp(35f, 105f, dragVisualProgress).toInt()
        thumbPaint.color = ColorUtils.setAlphaComponent(thumbColor, thumbAlpha)
        trackPaint.color = ColorUtils.setAlphaComponent(thumbColor, trackAlpha)
    }

    private fun edgeAlignedLeft(elementWidth: Float): Float =
        if (layoutDirection == LAYOUT_DIRECTION_RTL) 0f else width - elementWidth

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress

    private fun scrollToThumbTop(requestedTop: Float) {
        val list = recyclerView ?: return
        val travel = height - verticalMargin * 2f - thumbBounds.height()
        if (travel <= 0f) return

        val top = requestedTop.coerceIn(verticalMargin, verticalMargin + travel)
        val scrollableRange = list.computeVerticalScrollRange() - list.computeVerticalScrollExtent()
        val targetOffset = ((top - verticalMargin) / travel * scrollableRange).toInt()
        list.scrollBy(0, targetOffset - list.computeVerticalScrollOffset())
    }

    private fun updateThumbBounds() {
        val list = recyclerView ?: return
        val extent = list.computeVerticalScrollExtent()
        val range = list.computeVerticalScrollRange()
        val availableHeight = height - verticalMargin * 2f

        if (list.visibility != VISIBLE || height == 0 || range <= extent || extent <= 0 || availableHeight <= 0f) {
            thumbBounds.setEmpty()
            visibility = INVISIBLE
            invalidate()
            return
        }

        visibility = VISIBLE
        val thumbHeight = max(minimumThumbHeight, availableHeight * extent / range).coerceAtMost(availableHeight)
        val travel = availableHeight - thumbHeight
        val scrollableRange = range - extent
        val top = verticalMargin + travel * list.computeVerticalScrollOffset() / scrollableRange
        thumbBounds.set(0f, top, width.toFloat(), top + thumbHeight)
        invalidate()
    }
}
