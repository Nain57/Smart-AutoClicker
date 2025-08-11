
package com.buzbuz.smartautoclicker.core.ui.monitoring

import android.graphics.Point
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ViewMonitor(private val displayConfigManager: DisplayConfigManager) {

    private val onMonitoredViewLayoutChangedListener =
        OnGlobalLayoutListener {
            refreshViewSize()
        }

    private var monitoredView: View? = null
    private var positioningType: ViewPositioningType? = null

    private val _position: MutableStateFlow<Rect> = MutableStateFlow(Rect())
    val position: StateFlow<Rect> = _position

    fun attachView(view: View, type: ViewPositioningType) {
        monitoredView = view
        positioningType = type

        refreshViewSize()
        view.viewTreeObserver.addOnGlobalLayoutListener(onMonitoredViewLayoutChangedListener)
    }

    fun detachView() {
        monitoredView?.viewTreeObserver?.removeOnGlobalLayoutListener(onMonitoredViewLayoutChangedListener)
        monitoredView = null

        _position.value = Rect()
    }

    fun performClick(): Boolean =
        monitoredView?.performClick() ?: false

    private fun refreshViewSize() {
        val view = monitoredView ?: return
        val type = positioningType ?: return

        val location = when (type) {
            ViewPositioningType.WINDOW -> view.getLocationInWindow()
            ViewPositioningType.SCREEN -> view.getLocationOnScreen()
        }
        _position.value = Rect(location.x, location.y, location.x + view.width, location.y + view.height)
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