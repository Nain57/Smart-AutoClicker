
package com.buzbuz.smartautoclicker.core.domain.ext

import android.graphics.Bitmap
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition


suspend fun BitmapRepository.getConditionBitmap(
    condition: ImageCondition,
    targetWidth: Int = condition.area.width(),
    targetHeight: Int = condition.area.height(),
): Bitmap? = getImageConditionBitmap(
    path = condition.path,
    width = targetWidth,
    height = targetHeight,
)
