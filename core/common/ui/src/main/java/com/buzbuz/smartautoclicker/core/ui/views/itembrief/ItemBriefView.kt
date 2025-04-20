/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.itembrief

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.di.DisplayEntryPoint
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.DefaultBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.DefaultDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.conditions.ImageConditionBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.PauseBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.PauseDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.SwipeBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.SwipeDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.conditions.ImageConditionDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.conditions.TextConditionBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.conditions.TextConditionDescription
import dagger.hilt.EntryPoints


class ItemBriefView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Paint drawing the outer circle of the position 1. */
    private val style: ItemBriefViewStyle

    private val displayConfigManager: DisplayConfigManager by lazy {
        EntryPoints.get(context.applicationContext, DisplayEntryPoint::class.java)
            .displayMetrics()
    }

    init {
        if (attrs == null) throw IllegalArgumentException("AttributeSet is null")
        style = context.getItemBriefStyle(attrs, defStyleAttr)
    }

    private var renderer: ItemBriefRenderer<*>? = null
    private var description: ItemBriefDescription? = null

    /** Listener upon touch events */
    var onTouchListener: ((position: PointF) -> Unit)? = null

    fun setDescription(newDescription: ItemBriefDescription?, animate: Boolean = true) {
        if (description == newDescription) return

        val oldDescription = description
        description = newDescription
        val keepRenderer = renderer != null && oldDescription != null && newDescription != null
                && oldDescription::class.java == newDescription::class.java

        renderer?.onStop()
        if (!keepRenderer) {
            renderer = when (description) {
                is ClickDescription -> ClickBriefRenderer(this, style.clickStyle)
                is SwipeDescription -> SwipeBriefRenderer(this, style.swipeStyle)
                is PauseDescription -> PauseBriefRenderer(this, style.pauseStyle)
                is ImageConditionDescription -> ImageConditionBriefRenderer(
                    this, style.imageConditionStyle, displayConfigManager,
                )
                is TextConditionDescription -> TextConditionBriefRenderer(
                    this, style.imageConditionStyle, displayConfigManager,
                )
                is DefaultDescription -> DefaultBriefRenderer(this, style.defaultStyle)
                else -> null
            }

            Log.d(TAG, "Changing renderer to $renderer")
        }

        if (renderer == null || newDescription == null) {
            invalidate()
            return
        }

        renderer?.onNewDescription(newDescription, animate)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        onTouchListener?.invoke(event.getValidPosition()) ?: return false

        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) invalidate()
    }

    override fun invalidate() {
        renderer?.onInvalidate()
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer?.onDraw(canvas)
    }

    /** Get the position of the motion event and ensure it is within screen bounds. */
    private fun MotionEvent.getValidPosition(): PointF =
        PointF(
            x.coerceIn(0f, width.toFloat()),
            y.coerceIn(0f, height.toFloat()),
        )
}

interface ItemBriefDescription

private const val TAG = "ItemBriefView"