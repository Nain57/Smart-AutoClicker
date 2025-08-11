
package com.buzbuz.smartautoclicker.core.common.overlays.other

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator

import androidx.annotation.CallSuper
import androidx.annotation.StyleRes

import com.buzbuz.smartautoclicker.core.base.extensions.safeAddView
import com.buzbuz.smartautoclicker.core.common.overlays.base.BaseOverlay
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager

/** BaseOverlay class for an overlay displayed as full screen. */
abstract class FullscreenOverlay(@StyleRes theme: Int? = null) : BaseOverlay(theme) {

    /** The layout parameters of the window displaying the view. */
    private val viewLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        OverlayManager.OVERLAY_WINDOW_TYPE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    )

    /** The Android window manager. Used to add/remove the overlay menu and view. */
    private lateinit var windowManager: WindowManager
    /** The root view for this overlay; created by implementation via [onCreateView]. */
    private lateinit var view: View

    /** Animator for the overlay showing. */
    private val showAnimator: Animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 250
        interpolator = LinearInterpolator()
        addUpdateListener {
            view.alpha = it.animatedValue as Float
        }
    }

    protected abstract fun onCreateView(layoutInflater: LayoutInflater): View

    protected abstract fun onViewCreated()

    final override fun onCreate() {
        windowManager = context.getSystemService(WindowManager::class.java)
        view = onCreateView(context.getSystemService(LayoutInflater::class.java))

        onViewCreated()
    }

    @CallSuper
    override fun onStart() {
        view.alpha = 0f

        if (windowManager.safeAddView(view, viewLayoutParams)) {
            showAnimator.start()
        } else {
            finish()
        }
    }

    @CallSuper
    override fun onStop() {
        windowManager.removeView(view)
    }
}