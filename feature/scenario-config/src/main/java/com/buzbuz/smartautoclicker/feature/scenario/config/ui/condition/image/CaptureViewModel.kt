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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.image

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.display.DisplayRecorder
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptureViewModel(application: Application) : AndroidViewModel(application)  {

    /** Provides screen images. */
    private val displayRecorder: DisplayRecorder = DisplayRecorder.getInstance()
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    fun takeScreenshot(resultCallback: (Bitmap) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(200L)
            displayRecorder.takeScreenshot { screenshot ->
                withContext(Dispatchers.Main) {
                    resultCallback(screenshot)
                    monitoredViewsManager.notifyClick(MonitoredViewType.CONDITION_CAPTURE_BUTTON_CAPTURE)
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