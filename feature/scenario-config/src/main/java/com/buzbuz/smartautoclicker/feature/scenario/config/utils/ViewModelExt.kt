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
package com.buzbuz.smartautoclicker.feature.scenario.config.utils

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException


fun ViewModel.getImageConditionBitmap(repository: Repository, condition: ImageCondition, onCompleted: (Bitmap?) -> Unit): Job =
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val bitmap = repository.getConditionBitmap(condition)
            withContext(Dispatchers.Main) {
                onCompleted.invoke(bitmap)
            }
        } catch (cEx: CancellationException) {
            onCompleted.invoke(null)
        }
    }
