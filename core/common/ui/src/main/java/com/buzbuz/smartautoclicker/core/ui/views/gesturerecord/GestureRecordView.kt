
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
