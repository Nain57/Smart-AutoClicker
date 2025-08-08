
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