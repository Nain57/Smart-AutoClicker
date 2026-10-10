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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.utils.setColorIndicatorDrawable
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeScreenConditionVisualizationBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toEffectDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import kotlinx.coroutines.Job

fun IncludeScreenConditionVisualizationBinding.setCondition(
    condition: ScreenCondition,
    bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
): Job? =
    when (condition) {
        is ScreenCondition.Color -> {
            conditionImage.visibility = View.VISIBLE
            visualizationText.visibility = View.GONE

            conditionImage.setColorIndicatorDrawable(condition.color)
            null
        }

        is ScreenCondition.Image -> {
            conditionImage.visibility = View.VISIBLE
            visualizationText.visibility = View.GONE

            conditionImage.setImageBitmap(null)
            bitmapProvider.invoke(condition) { bitmap ->
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

        is ScreenCondition.Number -> {
            conditionImage.visibility = View.GONE
            visualizationText.visibility = View.VISIBLE

            visualizationText.text = condition.comparisonOperation
                .toEffectDescription(root.context, operand = condition.counterValue.toNaturalDisplayString())
            null
        }

        is ScreenCondition.Text ->  {
            conditionImage.visibility = View.GONE
            visualizationText.visibility = View.VISIBLE

            visualizationText.text = condition.text
            null
        }
    }
