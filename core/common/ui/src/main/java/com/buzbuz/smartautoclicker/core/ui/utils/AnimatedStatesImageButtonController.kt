/*
 * Copyright (C) 2023 Kevin Buzeau
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

