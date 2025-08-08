
package com.buzbuz.smartautoclicker.feature.smart.config.utils

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.ext.getConditionBitmap

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException


fun ViewModel.getImageConditionBitmap(repository: BitmapRepository, condition: ImageCondition, onCompleted: (Bitmap?) -> Unit): Job =
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val bitmap = repository.getConditionBitmap(condition)
            withContext(Dispatchers.Main) {
                onCompleted.invoke(bitmap)
            }
        } catch (cEx: CancellationException) {
            withContext(Dispatchers.Main) {
                onCompleted.invoke(null)
            }
        }
    }
