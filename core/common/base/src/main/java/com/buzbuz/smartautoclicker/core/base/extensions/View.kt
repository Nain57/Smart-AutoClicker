
package com.buzbuz.smartautoclicker.core.base.extensions

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.doOnLayout


fun View.doWhenMeasured(closure: () -> Unit) {
    if (width != 0 && height != 0) {
        closure()
        return
    }

    doOnLayout { doWhenMeasured(closure) }
}

fun View.delayDrawUntil(timeOutMs: Long = DEFAULT_DRAW_DELAY_TIMEOUT_MS, closure: () -> Boolean) {
    val timeOutTs = System.currentTimeMillis() + timeOutMs

    viewTreeObserver.addOnPreDrawListener(
        object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                return if (closure() || timeOutTs < System.currentTimeMillis()) {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    true
                } else {
                    false
                }
            }
        }
    )
}


private const val DEFAULT_DRAW_DELAY_TIMEOUT_MS = 3_000L