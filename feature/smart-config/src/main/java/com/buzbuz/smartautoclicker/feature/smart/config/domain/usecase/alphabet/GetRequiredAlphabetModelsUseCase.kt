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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.alphabet

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.OCRModelsRepository
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModel
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRequiredAlphabetModelsUseCase @Inject constructor(
    private val smartRepository: IRepository,
    private val ocrModelsRepository: OCRModelsRepository,
) {

    operator fun invoke(scenarioId: Long): Flow<List<OCRModel.Recognition>> = smartRepository
        .getScreenEventsFlow(scenarioId)
        .mapRequiredAlphabets()
        .mapModels(ocrModelsRepository.recognitionModels)

    private fun Flow<List<ScreenEvent>>.mapRequiredAlphabets(): Flow<Set<OCRAlphabet>> =
        map { events ->
            buildSet {
                events.forEach { event ->
                    event.conditions.forEach { condition ->
                        if (condition is ScreenCondition.Text) add(condition.alphabet)
                    }
                }
            }
        }

    private fun Flow<Set<OCRAlphabet>>.mapModels(models: Flow<Set<OCRModel.Recognition>>): Flow<List<OCRModel.Recognition>> =
        combine(models) { requiredSet, modelList ->
            buildList {
                requiredSet.forEach { required ->
                    modelList.find { model -> model.alphabet == required }?.let { model ->
                        add(model)
                    }
                }
            }
        }
}