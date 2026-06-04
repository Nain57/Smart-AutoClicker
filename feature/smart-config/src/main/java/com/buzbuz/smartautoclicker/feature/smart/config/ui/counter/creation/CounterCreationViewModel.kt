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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import javax.inject.Inject


class CountersCreationViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    fun addNewCounter(name: String, startingValue: Double) {
        val scenarioId = editionRepository.editionState.getScenario()?.id ?: return
        editionRepository.addNewCounter(
            Counter(
                counterName = name,
                defaultValue = startingValue,
                scenarioId = scenarioId,
            )
        )
    }
}
