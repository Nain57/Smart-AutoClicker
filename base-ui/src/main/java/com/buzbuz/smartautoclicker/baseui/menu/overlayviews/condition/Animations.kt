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
package com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition

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
 * Handles the animation for the [ConditionSelectorView].
 *
 * @param styledAttrs the styled attributes of the [ConditionSelectorView]
 */
internal class Animations(styledAttrs: TypedArray) {

    /** The transparency of the background color of the selector. */
    private val selectorBackgroundAlpha: Int = styledAttrs.getColor(
        R.styleable.ConditionSelectorView_colorBackground,
        Color.TRANSPARENT
    ).shr(24)
    /** The duration of the hints fading animation in milliseconds. */
    private val hintFadeDuration = styledAttrs.getInteger(
        R.styleable.ConditionSelectorView_hintsFadeDuration,
        DEFAULT_FADE_DURATION
    ).toLong()
    /** The duration of the all hints fading animation in milliseconds. */
    private val hintAllFadeDelay = styledAttrs.getInteger(
        R.styleable.ConditionSelectorView_hintsAllFadeDelay,
        DEFAULT_FADE_ALL_HINTS_DURATION
    ).toLong()
    /** The duration of the show selector animation in milliseconds. */
    private val showSelectorAnimationDuration = styledAttrs.getInteger(
        R.styleable.ConditionSelectorView_showSelectorAnimationDuration,
        DEFAULT_SELECTOR_ANIMATION_DURATION
    ).toLong()
    /** The duration of the show capture animation in milliseconds. */
    private val showCaptureAnimationDuration = styledAttrs.getInteger(
        R.styleable.ConditionSelectorView_showCaptureAnimationDuration,
        DEFAULT_CAPTURE_ANIMATION_DURATION
    ).toLong()

    /** Listener notified for capture zoom level changes due to animations. */
    var onCaptureZoomLevelChanged: ((Float) -> Unit)? = null
    /** Listener notified for selector border alpha changes due to animations. */
    var onSelectorBorderAlphaChanged: ((Int) -> Unit)? = null
    /** Listener notified for selector background alpha changes due to animations. */
    var onSelectorBackgroundAlphaChanged: ((Int) -> Unit)? = null
    /** Listener notified for hints alpha changes due to animations. */
    var onHintsAlphaChanged: ((Int) -> Unit)? = null

    /**
     * Start the show selector animation.
     *
     * @param onAnimationCompleted called once the animation is finished.
     */
    fun startShowSelectorAnimation(onAnimationCompleted: () -> Unit) {
        if (showSelectorAnimators.isRunning) {
            showSelectorAnimators.end()
        }

        showSelectorAnimators.doOnEnd { onAnimationCompleted.invoke() }
        showSelectorAnimators.start()
    }

    /** @return true if the show selector animation is running, false if not. */
    fun isShowSelectorAnimationRunning(): Boolean = showSelectorAnimators.isRunning

    /** Start the hide hints animation. */
    fun startHideHintsAnimation() {
        cancelHideHintsAnimation()
        hideHintsAnimator.start()
    }

    /** Cancel the hide hints animation. */
    fun cancelHideHintsAnimation() {
        if (hideHintsAnimator.isRunning) {
            hideHintsAnimator.end()
        }
    }

    /** Animator for the scale change when defining the capture. */
    private val showCaptureAnimator: Animator = ValueAnimator.ofFloat(1f, 0.8f).apply {
        duration = showCaptureAnimationDuration
        interpolator = DecelerateInterpolator(2f)
        addUpdateListener {
            onCaptureZoomLevelChanged?.invoke(it.animatedValue as Float)
        }
    }
    /** Animator for the selector and hints showing. */
    private val showSelectorAndHintsAnimator: Animator = ValueAnimator.ofInt(0, 255).apply {
        duration = showSelectorAnimationDuration
        interpolator = LinearInterpolator()
        addUpdateListener {
            (it.animatedValue as Int).let { alpha ->
                onSelectorBorderAlphaChanged?.invoke(alpha)
                onHintsAlphaChanged?.invoke(alpha)
            }
        }
    }
    /** Animator for the selector background showing. */
    private val showSelectorBackgroundAnimator: Animator = ValueAnimator.ofInt(0, selectorBackgroundAlpha).apply {
        duration = showSelectorAnimationDuration
        interpolator = LinearInterpolator()
        addUpdateListener {
            onSelectorBackgroundAlphaChanged?.invoke(it.animatedValue as Int)
        }
    }
    /** Set of animators required for the show selector animation. */
    private val showSelectorAnimators = AnimatorSet().apply {
        playTogether(
            listOf(
                showCaptureAnimator,
                showSelectorAndHintsAnimator,
                showSelectorBackgroundAnimator,
            )
        )
    }

    /** Animator for the hiding of the hints. */
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

/** The default duration of the hints fade out animation in milliseconds. */
private const val DEFAULT_FADE_DURATION = 500
/** The default duration of the all hints fade out animation in milliseconds. */
private const val DEFAULT_FADE_ALL_HINTS_DURATION = 1000
/** The default duration of the capture display animation in milliseconds. */
private const val DEFAULT_SELECTOR_ANIMATION_DURATION = 500
/** The default duration of the capture display animation in milliseconds. */
private const val DEFAULT_CAPTURE_ANIMATION_DURATION = 750