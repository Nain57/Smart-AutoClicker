/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui.overlayviews.condition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.res.TypedArray
import android.graphics.Color
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.buzbuz.smartautoclicker.ui.R

/**
 *
 * @param styledAttrs
 */
internal class Animations(styledAttrs: TypedArray) {

    private companion object {
        /** */
        private const val DEFAULT_FADE_DURATION = 500
        /** */
        private const val DEFAULT_FADE_ALL_HINTS_DURATION = 1000
        /** The duration of the capture display animation in milliseconds. */
        private const val SHOW_SELECTOR_ANIMATION_DURATION = 500L
        /** The duration of the capture display animation in milliseconds. */
        private const val SHOW_CAPTURE_ANIMATION_DURATION = 750L
    }

    /** */
    private val selectorBackgroundAlpha: Int = styledAttrs.getColor(
        R.styleable.ConditionSelectorView_colorBackground,
        Color.TRANSPARENT
    ).shr(24)

    /** */
    private val hintFadeDuration = styledAttrs.getInteger(
        R.styleable.ConditionSelectorView_hintsFadeDuration,
        DEFAULT_FADE_DURATION
    ).toLong()

    /** */
    private val hintAllFadeDelay = styledAttrs.getInteger(
        R.styleable.ConditionSelectorView_hintsAllFadeDelay,
        DEFAULT_FADE_ALL_HINTS_DURATION
    ).toLong()

    /** */
    var onCaptureZoomLevelChanged: ((Float) -> Unit)? = null
    /** */
    var onSelectorBorderAlphaChanged: ((Int) -> Unit)? = null
    /** */
    var onSelectorBackgroundAlphaChanged: ((Int) -> Unit)? = null
    /** */
    var onHintsAlphaChanged: ((Int) -> Unit)? = null

    /**
     *
     */
    fun startShowSelectorAnimation(onAnimationCompleted: () -> Unit) {
        if (showSelectorAnimators.isRunning) {
            showSelectorAnimators.end()
        }

        showSelectorAnimators.doOnEnd { onAnimationCompleted.invoke() }
        showSelectorAnimators.start()
    }

    /**
     * @return true if the show selector animation is running, false if not.
     */
    fun isShowSelectorAnimationRunning(): Boolean = showSelectorAnimators.isRunning

    /**
     *
     */
    fun startHideHintsAnimation() {
        cancelHideHintsAnimation()
        hideHintsAnimator.start()
    }

    fun cancelHideHintsAnimation() {
        if (hideHintsAnimator.isRunning) {
            hideHintsAnimator.end()
        }
    }

    /** Animator for the scale change when defining the capture. */
    private val showCaptureAnimator: Animator = ValueAnimator.ofFloat(1f, 0.8f).apply {
        duration = SHOW_CAPTURE_ANIMATION_DURATION
        interpolator = DecelerateInterpolator(2f)
        addUpdateListener {
            onCaptureZoomLevelChanged?.invoke(it.animatedValue as Float)
        }
    }

    /**  */
    private val showSelectorAndHintsAnimator: Animator = ValueAnimator.ofInt(0, 255).apply {
        duration = SHOW_SELECTOR_ANIMATION_DURATION
        interpolator = LinearInterpolator()
        addUpdateListener {
            (it.animatedValue as Int).let { alpha ->
                onSelectorBorderAlphaChanged?.invoke(alpha)
                onHintsAlphaChanged?.invoke(alpha)
            }
        }
    }

    /**  */
    private val showSelectorBackgroundAnimator: Animator = ValueAnimator.ofInt(0, selectorBackgroundAlpha).apply {
        duration = SHOW_SELECTOR_ANIMATION_DURATION
        interpolator = LinearInterpolator()
        addUpdateListener {
            onSelectorBackgroundAlphaChanged?.invoke(it.animatedValue as Int)
        }
    }

    /** */
    private val showSelectorAnimators = AnimatorSet().apply {
        playTogether(
            listOf(
                showCaptureAnimator,
                showSelectorAndHintsAnimator,
                showSelectorBackgroundAnimator,
            )
        )
    }

    /** */
    private val hideHintsAnimator = ValueAnimator.ofInt(255, 0).apply {
        startDelay = hintAllFadeDelay
        duration = hintFadeDuration
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
            onHintsAlphaChanged?.invoke(it.animatedValue as Int)
        }
        start()
    }
}