/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import com.buzbuz.smartautoclicker.core.ui.bindings.setTextOrGone
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldSliderBinding
import com.google.android.material.slider.Slider


fun IncludeFieldSliderBinding.setTitle(titleText: String) {
    title.setTextOrGone(titleText)
}

fun IncludeFieldSliderBinding.setValueLabelState(isEnabled: Boolean, prefix: String? = null) {
    val previousState = root.tag as? SliderState
    val previousListener = previousState?.valueListener

    if (previousListener != null) slider.removeOnChangeListener(previousListener)

    if (!isEnabled) {
        root.tag = previousState?.copy(valueListener = null, valuePrefix = prefix)
            ?: SliderState(valuePrefix = prefix)
        return
    }

    val listener = Slider.OnChangeListener { _, sliderValue, _ ->
        value.setTextOrGone(sliderValue.toInt().formatValueForSlider(prefix))
    }
    root.tag = previousState?.copy(valueListener = listener, valuePrefix = prefix)
        ?: SliderState(valueListener = listener, valuePrefix = prefix)
    slider.addOnChangeListener(listener)

    value.setTextOrGone(slider.value.toInt().formatValueForSlider(prefix))
}

fun IncludeFieldSliderBinding.setSliderValue(value: Float) {
    slider.value = value
}

fun IncludeFieldSliderBinding.setSliderRange(min: Float, max: Float) {
    slider.valueFrom = min
    slider.valueTo = max
}

fun IncludeFieldSliderBinding.setOnValueChangedFromUserListener(listener: (Float) -> Unit) {
    val previousState = root.tag as? SliderState
    val previousClientListener = previousState?.clientListener

    if (previousClientListener != null) slider.removeOnChangeListener(previousClientListener)

    val clientListener = Slider.OnChangeListener { _, sliderValue, fromUser ->
        if (fromUser) listener(sliderValue)
    }
    root.tag = previousState?.copy(clientListener = clientListener)
        ?: SliderState(clientListener = clientListener)
    slider.addOnChangeListener(clientListener)
}

private fun Int.formatValueForSlider(prefix: String?): String =
    prefix?.let { "$this $prefix" } ?: toString()

private data class SliderState(
    val valuePrefix: String? = null,
    val valueListener: Slider.OnChangeListener? = null,
    val clientListener: Slider.OnChangeListener? = null,
)
