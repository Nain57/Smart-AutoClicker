
package com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.utils.ExtendedValueAnimator
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

internal class ClickBriefRenderer(
    briefView: View,
    viewStyle: ClickBriefRendererStyle,
) : ItemBriefRenderer<ClickBriefRendererStyle>(briefView, viewStyle) {

    private val outerRadiusAnimator: ExtendedValueAnimator =
        ExtendedValueAnimator.ofFloat(viewStyle.outerRadiusPx, viewStyle.outerRadiusPx * 0.75f).apply {
            startDelay = 250
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            repeatDelay = 500
            addUpdateListener {
                animatedOuterRadius = (it.animatedValue as Float)
                invalidateView()
            }
        }

    private val gradientBackgroundPaint: Paint = Paint()

    private var briefDescription: ClickDescription? = null

    private var animatedOuterRadius: Float = viewStyle.outerRadiusPx
    private var position: PointF? = null
    private var conditionBitmap: Pair<Rect, Bitmap>? = null

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is ClickDescription) return

        if (outerRadiusAnimator.isStarted) outerRadiusAnimator.cancel()

        briefDescription = description
        animatedOuterRadius = viewStyle.outerRadiusPx

        if (animate) {
            outerRadiusAnimator.reverseDelay = description.pressDurationMs
            outerRadiusAnimator.start()
        }
    }

    override fun onInvalidate() {
        conditionBitmap = null
        val description = briefDescription ?: return

        val viewSize = getViewSize()
        val bitmap = description.imageConditionBitmap
        if (bitmap != null) {
            val left = (viewSize.x - bitmap.width) / 2
            val top = (viewSize.y - bitmap.height) / 2
            conditionBitmap = Pair(
                Rect(left, top, left + bitmap.width, top + bitmap.height),
                description.imageConditionBitmap,
            )
            position = PointF(viewSize.x / 2f, viewSize.y / 2f)

        } else {
            position = description.position
        }


        position?.let { clickPosition ->
            gradientBackgroundPaint.shader = createRadialGradientShader(
                position = clickPosition,
                radius = viewStyle.outerRadiusPx * 1.75f,
                color = viewStyle.backgroundColor,
            )
        }
    }

    override fun onStop() {
        outerRadiusAnimator.cancel()
        animatedOuterRadius = viewStyle.outerRadiusPx
        position = null
    }

    override fun onDraw(canvas: Canvas) {
        position?.let { pos ->
            conditionBitmap?.let { (screenPosition, bitmap) ->
                canvas.drawBitmap(bitmap, null, screenPosition, null)
            }

            canvas.drawCircle(pos.x, pos.y, animatedOuterRadius * 2f, gradientBackgroundPaint)
            canvas.drawCircle(pos.x, pos.y, animatedOuterRadius, viewStyle.outerPaint)
            canvas.drawCircle(pos.x, pos.y, viewStyle.innerRadiusPx, viewStyle.innerPaint)
        }
    }
}

data class ClickDescription(
    val pressDurationMs: Long = MINIMAL_ANIMATION_DURATION_MS,
    val position: PointF? = null,
    val imageConditionBitmap: Bitmap? = null
) : ItemBriefDescription


internal data class ClickBriefRendererStyle(
    @ColorInt val backgroundColor: Int,
    val outerPaint: Paint,
    val innerPaint: Paint,
    val outerRadiusPx: Float,
    val innerRadiusPx: Float,
)

private const val MINIMAL_ANIMATION_DURATION_MS = 1L
