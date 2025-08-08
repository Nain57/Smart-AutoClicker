
package com.buzbuz.smartautoclicker.core.ui.views.conditionselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent
import android.view.MotionEvent

import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.CaptureComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.SelectorComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ComponentsView
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.HintsComponent

/**
 * Overlay view used as screenOverlayView showing the area to capture the content as an event condition.
 * This view allows to zoom/move the bitmap displayed as background, as well as display a selector over it allowing to
 * easily select a section of the screen for a event condition.
 *
 * @param context the Android context
 * @param displayConfigManager the current screen metrics.
 * @param onSelectorValidityChanged listener upon the selector validity.
 */
@SuppressLint("ViewConstructor") // Not intended to be used from XML
class ConditionSelectorView(
    context: Context,
    private val displayConfigManager: DisplayConfigManager,
    private val onSelectorValidityChanged: (Boolean) -> Unit,
) : ComponentsView(context) {

    /** Controls the display of the bitmap captured. */
    private lateinit var capture: CaptureComponent
    /** Controls the display of the selector. */
    private lateinit var selector: SelectorComponent
    /** Controls the display of the user hints around the selector. */
    private lateinit var hintsIcons: HintsComponent
    /** Controls the animations. */
    private lateinit var animations: ConditionSelectorAnimations

    /** Tells if the view have ignored a touch event due to a animation running or being hidden. */
    private var haveTouchEventIgnored = false
    /** Tells if the selector is at a valid position relatively to the capture position. */
    private var isSelectorValid = false
    /** Used during selector validation. kept here to avoid instantiation at each touch event. */
    private val selectorValidityTempValue = RectF()

    /** Get the attributes from the style file and initialize all components. */
    init {
        context.obtainStyledAttributes(null, R.styleable.ConditionSelectorView, R.attr.conditionSelectorStyle, 0).use { ta ->
            animations = ConditionSelectorAnimations(ta.getAnimationsStyle())
            capture = CaptureComponent(context, ta.getCaptureComponentStyle(displayConfigManager), this)
            selector = SelectorComponent(context, ta.getSelectorComponentStyle(displayConfigManager), this)
            hintsIcons = HintsComponent(context, ta.getHintsStyle(displayConfigManager), this)
        }
    }

    /** Setup the position changes callbacks. */
    init {
        selector.onSelectorPositionChanged = { position ->
            hintsIcons.setSelectorArea(position)
            verifySelectorValidity()
        }
        capture.onCapturePositionChanged = { _ ->
            verifySelectorValidity()
        }
    }

    /** Setup animation values callback. */
    init {
        animations.apply {
            onCaptureZoomLevelChanged = { zoomLevel ->
                capture.setZoomLevel(zoomLevel)
            }
            onSelectorBorderAlphaChanged = { alpha ->
                selector.selectorAlpha = alpha
            }
            onSelectorBackgroundAlphaChanged = { alpha ->
                selector.backgroundAlpha = alpha
            }
            onHintsAlphaChanged = { alpha ->
                hintsIcons.alpha = alpha
            }
        }
    }

    /** Tell if the content of this view should be hidden or not. */
    var hide = true
        set(value) {
            if (field == value) {
                return
            }
            field = value

            if (value) {
                capture.onReset()
                selector.onReset()
                hintsIcons.onReset()
            }
            invalidate()
        }

    override val viewComponents: List<ViewComponent> = listOf(capture, selector, hintsIcons)

    /**
     * Verifies if the [selector] is at a valid position with the [capture].
     * If the validation position value changes, notifies [onSelectorValidityChanged].
     */
    private fun verifySelectorValidity() {
        selectorValidityTempValue.set(RectF(selector.selectedArea))

        val isSelectorOverCapture = selectorValidityTempValue.intersect(capture.captureArea)
        val isBiggerThanMinimumSize = selectorValidityTempValue.width() >= CAPTURE_MINIMUM_SIZE
                && selectorValidityTempValue.height() >= CAPTURE_MINIMUM_SIZE

        if ((isSelectorOverCapture && isBiggerThanMinimumSize) != isSelectorValid) {
            isSelectorValid = !isSelectorValid
            onSelectorValidityChanged(isSelectorValid)
        }
    }

    /**
     * Shows the capture on the screen.
     *
     * @param bitmap the capture the be shown.
     */
    fun showCapture(bitmap: Bitmap) {
        capture.screenCapture = BitmapDrawable(resources, bitmap)
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
        if (!isSelectorValid) throw IllegalStateException("Can't get a selection, selector is invalid.")

        return capture.screenCapture
            ?.getSelection(selector.getSelectionArea(capture.captureArea, capture.zoomLevel))
            ?: throw IllegalStateException("Can't get a selection, there is no screen capture.")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || hide) {
            return false
        }

        // Refresh the hints depending on the last gesture detected for the selector
        selector.currentGesture?.let { gestureType ->
            hintsIcons.show(gestureType)
            animations.cancelHideHintsAnimation()

            if (event.action == KeyEvent.ACTION_UP) {
                animations.startHideHintsAnimation()
            }
        }

        // If the selector consume the event, return now
        if (selector.onTouchEvent(event)) return true

        // The event is on the capture and it's animating, ignore the event.
        if (animations.isShowSelectorAnimationRunning()) {
            haveTouchEventIgnored = true
            return false
        }

        // An event was ignored, force this first event to down
        if (haveTouchEventIgnored) {
            event.action = KeyEvent.ACTION_DOWN
            haveTouchEventIgnored = false
        }

        return capture.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        if (hide) return
        super.onDraw(canvas)
    }

    private fun BitmapDrawable.getSelection(area: Rect): Pair<Rect, Bitmap>? {
        val captureArea = Rect(0, 0, bitmap.width, bitmap.height)
        if (!captureArea.intersect(area)) return null

        return captureArea to Bitmap.createBitmap(bitmap, captureArea.left, captureArea.top, captureArea.width(), captureArea.height())
    }
}

/**
 * The minimum size of the capture.
 * Final results will not always be the size, as it is relative to the capture viewport, but we just don't want a
 * null result.
 */
private const val CAPTURE_MINIMUM_SIZE = 50f