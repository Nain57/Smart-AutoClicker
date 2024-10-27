/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.gesturerecord

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.di.DisplayEntryPoint
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DisplayBorderComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DisplayBorderComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ComponentsView
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent

import dagger.hilt.EntryPoints


class GestureRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ComponentsView(context, attrs, defStyleAttr) {

    private val displayConfigManager: DisplayConfigManager by lazy {
        EntryPoints.get(context.applicationContext, DisplayEntryPoint::class.java)
            .displayMetrics()
    }

    private val viewStyle = context
        .obtainStyledAttributes(null, R.styleable.GestureRecordView, R.attr.gestureRecordStyle, 0)
        .use { ta -> ta.getGestureRecorderStyle()}

    private val gestureRecorder = GestureRecorder { gesture, isFinished ->
        gestureCaptureListener?.invoke(gesture, isFinished)
    }

    var gestureCaptureListener: ((gesture: RecordedGesture?, isFinished: Boolean) -> Unit)? = null

    override val viewComponents: List<ViewComponent> = listOf(
        DisplayBorderComponent(
            viewStyle = DisplayBorderComponentStyle(
                displayConfigManager = displayConfigManager,
                color = viewStyle.color,
                thicknessPx = viewStyle.thicknessPx,
            ),
            viewInvalidator = this,
        ),
    )

    fun clearAndHide() {
        visibility = GONE
        gestureRecorder.clearCapture()
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        return gestureRecorder.processEvent(event)
    }
}
