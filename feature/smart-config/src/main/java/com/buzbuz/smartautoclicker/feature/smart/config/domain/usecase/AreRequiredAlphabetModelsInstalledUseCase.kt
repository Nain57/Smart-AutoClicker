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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModelState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class AreRequiredAlphabetModelsInstalledUseCase @Inject constructor(
    private val getRequiredAlphabetModelsUseCase: GetRequiredAlphabetModelsUseCase,
) {

    operator fun invoke(scenarioId: Long): Flow<Boolean> =
        getRequiredAlphabetModelsUseCase(scenarioId)
            .map { models ->
                models.find { model -> model.state !is OCRModelState.Installed } == null
            }

}

