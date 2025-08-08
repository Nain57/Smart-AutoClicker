
package com.buzbuz.smartautoclicker.core.ui.views.itembrief

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

import androidx.annotation.ColorInt

internal abstract class ItemBriefRenderer<Style>(
    protected val briefView: View,
    protected val viewStyle: Style,
) {
    abstract fun onNewDescription(description: ItemBriefDescription, animate: Boolean)
    abstract fun onInvalidate()
    abstract fun onDraw(canvas: Canvas)
    abstract fun onStop()

    protected fun getViewSize(): Point =
        Point(briefView.width, briefView.height)

    protected fun invalidateView() { briefView.invalidate() }

    protected fun createRadialGradientShader(position: PointF, radius: Float, @ColorInt color: Int): Shader =
        RadialGradient(
            position.x,
            position.y,
            radius,
            color,
            color.setAlpha(0),
            Shader.TileMode.CLAMP,
        )

    @ColorInt
    private fun Int.setAlpha(alpha: Int): Int =
        Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
}