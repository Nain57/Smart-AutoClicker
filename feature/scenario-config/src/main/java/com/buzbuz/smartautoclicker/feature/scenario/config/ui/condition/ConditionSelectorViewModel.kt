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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.display.ScreenRecorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConditionSelectorViewModel(application: Application) : AndroidViewModel(application)  {

    /** Provides screen images. */
    private val screenRecorder: ScreenRecorder = ScreenRecorder.getInstance()

    fun takeScreenshot(area: Rect, resultCallback: (Bitmap) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(200L)
            screenRecorder.takeScreenshot(area) { screenshot ->
                withContext(Dispatchers.Main) {
                    resultCallback(screenshot)
                }
            }
        }
    }
}