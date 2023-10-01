/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.areaselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

import androidx.core.content.res.use
import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.SelectorComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.HintsComponent

@SuppressLint("ViewConstructor") // Not intended to be used from XML
class AreaSelectorView(
    context: Context,
    private val displayMetrics: DisplayMetrics,
) : View(context) {

    /** Controls the display of the selector. */
    private lateinit var selector: SelectorComponent
    /** Controls the display of the user hints around the selector. */
    private lateinit var hintsIcons: HintsComponent
    /** Controls the animations. */
    private lateinit var animations: AreaSelectorAnimations

    /** Tells if the view have ignored a touch event due to a animation running or being hidden. */
    private var haveTouchEventIgnored = false

    /** Get the attributes from the style file and initialize all components. */
    init {
        context.obtainStyledAttributes(null, R.styleable.AreaSelectorView, R.attr.areaSelectorStyle, 0).use { ta ->
            animations = AreaSelectorAnimations(ta.getAnimationsStyle())
            selector = SelectorComponent(context, ta.getSelectorComponentStyle(displayMetrics), ::invalidate)
            hintsIcons = HintsComponent(context, ta.getHintsStyle(displayMetrics), ::invalidate)
        }
    }

    /** Setup the position changes callback. */
    init {
        selector.onSelectorPositionChanged = { position ->
            hintsIcons.setSelectorArea(position)
        }
    }

    /** Setup animation values callback. */
    init {
        animations.apply {
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

    fun setSelection(area: Rect, minimalArea: Rect) {
        if (selector.setDefaultSelectionArea(area, minimalArea)) {
            hintsIcons.showAll()
            animations.startShowSelectorAnimation(
                onAnimationCompleted = { animations.startHideHintsAnimation() }
            )
        }
    }

    fun getSelection(): Rect =
        selector.selectedArea.toRect()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        selector.onViewSizeChanged(w, h)
        hintsIcons.onViewSizeChanged(w, h)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

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

        return false
    }

    override fun onDraw(canvas: Canvas) {
        selector.onDraw(canvas)
        hintsIcons.onDraw(canvas)
    }
}