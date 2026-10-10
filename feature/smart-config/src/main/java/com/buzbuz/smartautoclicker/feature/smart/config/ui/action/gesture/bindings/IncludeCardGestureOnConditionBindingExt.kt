/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeCardGestureOnConditionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.setCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getClickOnConditionDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getClickOnConditionTitle

import kotlinx.coroutines.Job

fun IncludeCardGestureOnConditionBinding.bindCondition(
    condition: ScreenCondition?,
    inError: Boolean,
    bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
): Job? {
    root.isVisible = true

    conditionText.text = root.context.getClickOnConditionTitle(condition, inError)
    conditionDesc.text = root.context.getClickOnConditionDescription(condition, inError)

    if (inError) {
        conditionVisualization.visualizationText.isVisible = false
        conditionVisualization.conditionImage.isVisible = true
        conditionVisualization.conditionImage.setImageDrawable(
            ContextCompat.getDrawable(root.context, R.drawable.ic_cancel)?.apply {
                setTint(Color.RED)
            }
        )
    } else if (condition == null) {
        conditionVisualization.visualizationText.isVisible = false
        conditionVisualization.conditionImage.isVisible = true
        conditionVisualization.conditionImage.setImageResource(R.drawable.ic_screen_event)
    } else {
        return conditionVisualization.setCondition(condition, bitmapProvider)
    }

    return null
}