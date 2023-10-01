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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings

import android.graphics.Bitmap
import android.graphics.Color

import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemConditionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.setIconTint

import kotlinx.coroutines.Job

/**
 * Bind the [ItemConditionBinding] to a condition.
 */
fun ItemConditionBinding.bind(
    condition: Condition,
    bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    conditionClickedListener: (Condition) -> Unit
): Job? {
    root.setOnClickListener { conditionClickedListener.invoke(condition) }

    conditionName.text = condition.name

    conditionShouldBeDetected.apply {
        if (condition.shouldBeDetected) {
            setImageResource(R.drawable.ic_confirm)
            setIconTint(R.color.overlayMenuButtons)
        } else {
            setImageResource(R.drawable.ic_cancel)
            setIconTint(R.color.overlayMenuButtons)
        }
    }

    conditionDetectionType.apply {
        setImageResource(
            when (condition.detectionType) {
                EXACT -> R.drawable.ic_detect_exact
                WHOLE_SCREEN -> R.drawable.ic_detect_whole_screen
                IN_AREA -> R.drawable.ic_detect_in_area
                else -> return@apply
            }
        )
        setIconTint(R.color.overlayMenuButtons)
    }


    conditionThreshold.text = root.context.getString(
        R.string.message_condition_threshold,
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