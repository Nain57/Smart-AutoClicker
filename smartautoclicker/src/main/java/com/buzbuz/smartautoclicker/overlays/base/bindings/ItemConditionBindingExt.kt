/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.base.bindings

import android.graphics.Bitmap
import android.graphics.Color

import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemConditionBinding
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.overlays.base.utils.setIconTint

import kotlinx.coroutines.Job

/**
 * Bind the [ItemConditionBinding] to a condition.
 */
fun ItemConditionBinding.bind(
    condition: Condition,
    bindingAdapterPosition: Int,
    bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    conditionClickedListener: (Condition, Int) -> Unit
): Job? {
    root.setOnClickListener { conditionClickedListener.invoke(condition, bindingAdapterPosition) }

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
            if (condition.detectionType == EXACT) R.drawable.ic_detect_exact else R.drawable.ic_detect_whole_screen
        )
        setIconTint(R.color.overlayMenuButtons)
    }


    conditionThreshold.text = root.context.getString(
        R.string.dialog_condition_copy_threshold,
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