
package com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

abstract class ComponentsView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), ViewInvalidator {

    internal abstract val viewComponents: List<ViewComponent>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewComponents.forEach { viewComponent -> viewComponent.onViewSizeChanged(w, h) }
        invalidate()
    }

    override fun invalidate() {
        viewComponents.forEach { viewComponent -> viewComponent.onInvalidate() }
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        viewComponents.forEach { viewComponent -> viewComponent.onDraw(canvas) }
    }
}

interface ViewInvalidator {
    fun invalidate()
}