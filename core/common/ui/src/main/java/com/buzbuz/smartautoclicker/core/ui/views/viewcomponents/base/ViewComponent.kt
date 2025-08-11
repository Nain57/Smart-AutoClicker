
package com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base

import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent

import androidx.annotation.CallSuper

/**
 * Base class for all view components displayed in a View.
 *
 * @param viewComponentStyle provides information about the style to apply to the component.
 * @param viewInvalidator invalidate the parent view
 */
internal abstract class ViewComponent(
    private val viewComponentStyle: ViewStyle,
    private val viewInvalidator: ViewInvalidator,
) {

    /** The maximum size of the selector. */
    protected val maxArea: RectF = RectF().apply {
        val screenSize = viewComponentStyle.displayConfigManager.displayConfig.sizePx
        right = screenSize.x.toFloat()
        bottom = screenSize.y.toFloat()
    }

    /**
     * Called when the size of the View have changed.
     * Update the maximum area. Can be overridden to clear/adjust the displayed component position.
     *
     * @param w the width of the new view.
     * @param h the height of the new view.
     */
    @CallSuper
    open fun onViewSizeChanged(w: Int, h: Int) {
        val screenSize = viewComponentStyle.displayConfigManager.displayConfig.sizePx
        maxArea.apply {
            right = screenSize.x.toFloat()
            bottom = screenSize.y.toFloat()
        }
    }

    /** Invalidates the view containing the component. */
    protected fun invalidate() = viewInvalidator.invalidate()

    /**
     * Called when a touch event occurs in the View.
     *
     * @param event the new touch event.
     * @return true if the event has been consumed, false if not.
     */
    abstract fun onTouchEvent(event: MotionEvent): Boolean

    /** Called when the view or one of it's component have been updated and drawing cache needs to be recomputed. */
    abstract fun onInvalidate()

    /**
     * Called when the view needs to draw this component.
     *
     * @param canvas the canvas to draw in.
     */
    abstract fun onDraw(canvas: Canvas)

    /**
     * Called when this components needs to be reset (like after a cancel).
     * All temporary values should be dropped and the component should returns to its initial state.
     */
    abstract fun onReset()
}