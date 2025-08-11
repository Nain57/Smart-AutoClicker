
package com.buzbuz.smartautoclicker.core.base.extensions

import android.view.animation.Animation

fun Animation.setListener(start: (() -> Unit)? = null, end: (() -> Unit)? = null) {
    setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) { start?.invoke() }
        override fun onAnimationRepeat(animation: Animation?) = Unit
        override fun onAnimationEnd(animation: Animation?) { end?.invoke() }
    })
}