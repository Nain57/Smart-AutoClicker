
package com.buzbuz.smartautoclicker.core.ui.utils

import android.content.Context
import android.widget.ImageButton

import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat


/** Handles the states of an [ImageButton] and their animated transitions. */
class AnimatedStatesImageButtonController(
    context: Context,
    @DrawableRes private val state1StaticRes: Int,
    @DrawableRes private val state2StaticRes: Int,
    @DrawableRes state1to2AnimationRes: Int,
    @DrawableRes state2to1AnimationRes: Int,
) {

    /** Animation from state 1 to state 2. */
    private val state1to2Animation: AnimatedVectorDrawableCompat =
        AnimatedVectorDrawableCompat.create(context, state1to2AnimationRes)!!
    /** Animation from state 2 to state 1. */
    private val state2to1Animation: AnimatedVectorDrawableCompat =
        AnimatedVectorDrawableCompat.create(context, state2to1AnimationRes)!!

    private var button: ImageButton? = null

    fun attachView(view: ImageButton) {
        button = view
    }

    fun detachView() {
        button = null
    }

    fun toState1(animate: Boolean) {
        button?.let { buttonView ->
            if (animate) {
                buttonView.setImageDrawable(state2to1Animation)
                state2to1Animation.start()
            } else {
                buttonView.setImageResource(state1StaticRes)
            }
        }
    }

    fun toState2(animate: Boolean) {
        button?.let { buttonView ->
            if (animate) {
                buttonView.setImageDrawable(state1to2Animation)
                state1to2Animation.start()
            } else {
                buttonView.setImageResource(state2StaticRes)
            }
        }
    }
}

