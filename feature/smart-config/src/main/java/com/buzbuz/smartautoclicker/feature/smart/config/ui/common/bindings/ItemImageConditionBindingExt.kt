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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import android.graphics.Bitmap
import android.graphics.Color

import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeImageConditionCardBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition

import kotlinx.coroutines.Job

/**
 * Bind the [IncludeImageConditionCardBinding] to a condition.
 */
fun IncludeImageConditionCardBinding.bind(
    uiCondition: UiImageCondition,
    bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    conditionClickedListener: (ImageCondition) -> Unit
): Job? {
    root.setOnClickListener { conditionClickedListener.invoke(uiCondition.condition) }

    conditionName.text = uiCondition.name
    conditionShouldBeDetected.setImageResource(uiCondition.shouldBeVisibleIconRes)
    conditionDetectionType.setImageResource(uiCondition.detectionTypeIconRes)
    conditionThreshold.text = uiCondition.thresholdText

    return bitmapProvider.invoke(uiCondition.condition) { bitmap ->
        if (bitmap != null) {
            conditionImage.setImageBitmap(bitmap)
        } else {
            conditionImage.setImageDrawable(
                ContextCompat.getDrawable(root.context, R.drawable.ic_cancel)?.apply {
                    setTint(Color.RED)
                }
            )
        }
    }
}