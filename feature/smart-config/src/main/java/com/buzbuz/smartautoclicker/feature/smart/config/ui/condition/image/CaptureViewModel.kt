
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.display.recorder.DisplayRecorder
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CaptureViewModel @Inject constructor(
    private val displayRecorder: DisplayRecorder,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel()  {

    fun takeScreenshot(resultCallback: (Bitmap) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(200L)
            displayRecorder.takeScreenshot { screenshot ->
                withContext(Dispatchers.Main) {
                    resultCallback(screenshot)
                    monitoredViewsManager.notifyClick(MonitoredViewType.CONDITION_CAPTURE_MENU_BUTTON_CAPTURE)
                }
            }
        }
    }

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param area the area of the condition to create.
     * @param bitmap the image for the condition to create.
     */
    fun createImageCondition(context: Context, area: Rect, bitmap: Bitmap, completed: (ImageCondition) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val condition = editionRepository.editedItemsBuilder.createNewImageCondition(context, area, bitmap)
            withContext(Dispatchers.Main) { completed(condition) }
        }
    }
}