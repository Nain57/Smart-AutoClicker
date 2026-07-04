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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring

import android.graphics.Point
import android.graphics.Rect
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewParent
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.EditText
import androidx.core.widget.NestedScrollView

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class ViewMonitor(private val displayConfigManager: DisplayConfigManager) {

    private val onMonitoredViewLayoutChangedListener =
        OnGlobalLayoutListener {
            refreshViewSize()
            scheduleSettledVisibilityCheck()
        }
    private val onMonitoredViewScrollChangedListener =
        OnScrollChangedListener {
            refreshViewSize()
        }

    private val editTextListener: TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) { _text.update { s?.toString() } }
        }

    private val settledVisibilityCheckRunnable = Runnable {
        monitoredView?.scrollIntoViewIfNeeded()
    }

    private var monitoredView: View? = null
    private var positioningType: ViewPositioningType? = null
    private var attachTimeMs: Long = 0L

    private val _position: MutableStateFlow<Rect> = MutableStateFlow(Rect())
    val position: StateFlow<Rect> = _position

    private val _text: MutableStateFlow<String?> = MutableStateFlow(null)
    val text: StateFlow<String?> = _text


    fun attachView(view: View, type: ViewPositioningType) {
        monitoredView = view
        positioningType = type
        attachTimeMs = SystemClock.uptimeMillis()

        refreshViewSize()
        view.viewTreeObserver.addOnGlobalLayoutListener(onMonitoredViewLayoutChangedListener)
        view.viewTreeObserver.addOnScrollChangedListener(onMonitoredViewScrollChangedListener)

        if (view is EditText) view.addTextChangedListener(editTextListener)

        scheduleSettledVisibilityCheck()
    }

    fun detachView() {
        monitoredView?.let { view ->
            view.viewTreeObserver.removeOnGlobalLayoutListener(onMonitoredViewLayoutChangedListener)
            view.viewTreeObserver.removeOnScrollChangedListener(onMonitoredViewScrollChangedListener)
            view.removeCallbacks(settledVisibilityCheckRunnable)

            if (view is EditText) view.removeTextChangedListener(editTextListener)
        }
        monitoredView = null

        _position.value = Rect()
        _text.update { null }
    }

    fun performClick(): Boolean =
        if (monitoredView?.isEnabled == true) monitoredView?.performClick() ?: false
        else false


    private fun refreshViewSize() {
        val view = monitoredView ?: return
        val type = positioningType ?: return

        val location = when (type) {
            ViewPositioningType.WINDOW -> view.getLocationInWindow()
            ViewPositioningType.SCREEN -> view.getLocationOnScreen()
        }
        _position.value = Rect(location.x, location.y, location.x + view.width, location.y + view.height)
    }

    /**
     * The view's/its parents' geometry keeps shifting for a short while after attach (e.g. attach() called from
     * RecyclerView.onBindViewHolder before the item is positioned, async content resizing a card, IME insets
     * settling), so re-check visibility a bit after each layout pass instead of only once, until things settle.
     */
    private fun scheduleSettledVisibilityCheck() {
        val view = monitoredView ?: return
        if (SystemClock.uptimeMillis() - attachTimeMs > SETTLE_WINDOW_MS) return

        view.removeCallbacks(settledVisibilityCheckRunnable)
        view.postDelayed(settledVisibilityCheckRunnable, SETTLE_CHECK_DELAY_MS)
    }

    /** If this view is laid out but partially/fully hidden by a scrollable ancestor, ask it to scroll into view. */
    private fun View.scrollIntoViewIfNeeded() {
        if (width == 0 || height == 0) return

        val visibleRect = Rect()
        val isFullyVisible = getLocalVisibleRect(visibleRect) && visibleRect == Rect(0, 0, width, height)
        if (isFullyVisible) return

        // Scroll the NestedScrollView ourselves rather than relying on requestRectangleOnScreen: its propagation
        // through a BottomSheetDialog's CoordinatorLayout is not reliable enough to guarantee the view ends up
        // fully visible.
        val nestedScrollView = findNestedScrollViewAncestor()
        if (nestedScrollView != null) nestedScrollView.scrollToShow(this)
        else requestRectangleOnScreen(Rect(0, 0, width, height), true)
    }

    private fun View.findNestedScrollViewAncestor(): NestedScrollView? {
        var current: ViewParent? = parent
        while (current is View) {
            if (current is NestedScrollView) return current
            current = current.parent
        }
        return null
    }

    private fun NestedScrollView.scrollToShow(view: View) {
        val rect = Rect(0, 0, view.width, view.height)
        offsetDescendantRectToMyCoords(view, rect)

        val targetScrollY = when {
            rect.height() > height || rect.top < scrollY -> rect.top
            rect.bottom > scrollY + height -> rect.bottom - height
            else -> return
        }
        post { smoothScrollTo(scrollX, targetScrollY) }
    }

    private fun View.getLocationInWindow(): Point {
        val location = IntArray(2)
        getLocationInWindow(location)
        return Point(location[0], location[1])
    }

    private fun View.getLocationOnScreen(): Point {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return Point(location[0], location[1] -  displayConfigManager.displayConfig.safeInsetTopPx)
    }
}

enum class ViewPositioningType {
    WINDOW,
    SCREEN,
}


/** How long after attach a monitored view's container keeps reflowing (e.g. async content loading, IME insets
 *  settling) before we stop trying to scroll it into view, so we never fight a user's own manual scrolling. */
private const val SETTLE_WINDOW_MS = 2_000L
private const val SETTLE_CHECK_DELAY_MS = 150L