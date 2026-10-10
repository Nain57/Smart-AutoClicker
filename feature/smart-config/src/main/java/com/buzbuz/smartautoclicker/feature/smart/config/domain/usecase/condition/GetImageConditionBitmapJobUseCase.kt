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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.condition

import android.graphics.Bitmap

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.ext.getConditionBitmap
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


class GetImageConditionBitmapJobUseCase @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @param:Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
    private val bitmapRepository: BitmapRepository,
) {

    operator fun invoke(
        scope: CoroutineScope,
        condition: ScreenCondition.Image,
        onCompleted: (Bitmap?) -> Unit,
    ): Job = scope.launch(ioDispatcher) {
        try {
            val bitmap = bitmapRepository.getConditionBitmap(condition)
            withContext(mainDispatcher) {
                onCompleted.invoke(bitmap)
            }
        } catch (_: CancellationException) {
            withContext(mainDispatcher) {
                onCompleted.invoke(null)
            }
        }
    }
}