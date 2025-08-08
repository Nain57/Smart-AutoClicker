
package com.buzbuz.smartautoclicker.core.ui.views.gesturerecord

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.SwipeDescription


sealed class RecordedGesture {
    abstract val durationMs: Long

    data class Click(
        val position: PointF,
        override val durationMs: Long,
    ) : RecordedGesture()

    data class Swipe(
        val from: PointF,
        val to: PointF,
        override val durationMs: Long,
    ) : RecordedGesture()
}

fun RecordedGesture.toActionDescription(): ItemBriefDescription =
    when (this) {
        is RecordedGesture.Click -> ClickDescription(pressDurationMs = durationMs, position = position)
        is RecordedGesture.Swipe -> SwipeDescription(swipeDurationMs = durationMs, from = from, to = to)
    }