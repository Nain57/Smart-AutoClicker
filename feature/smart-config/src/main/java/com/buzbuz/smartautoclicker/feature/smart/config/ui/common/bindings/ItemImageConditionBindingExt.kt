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
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeImageConditionCardBinding

import kotlinx.coroutines.Job

/**
 * Bind the [IncludeImageConditionCardBinding] to a condition.
 */
fun IncludeImageConditionCardBinding.bind(
    condition: ImageCondition,
    bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    conditionClickedListener: (ImageCondition) -> Unit
): Job? {
    root.setOnClickListener { conditionClickedListener.invoke(condition) }

    conditionName.text = condition.name

    conditionShouldBeDetected.setImageResource(
        if (condition.shouldBeDetected) R.drawable.ic_confirm
        else R.drawable.ic_cancel
    )

    conditionDetectionType.apply {
        setImageResource(
            when (condition.detectionType) {
                EXACT -> R.drawable.ic_detect_exact
                WHOLE_SCREEN -> R.drawable.ic_detect_whole_screen
                IN_AREA -> R.drawable.ic_detect_in_area
                else -> return@apply
            }
        )
    }

    conditionThreshold.text = root.context.getString(
        R.string.item_image_condition_desc_threshold,
        condition.threshold
    )

    return bitmapProvider.invoke(condition) { bitmap ->
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