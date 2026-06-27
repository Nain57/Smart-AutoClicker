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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection

import android.content.Context
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CounterSelectionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    editionRepository: EditionRepository,
) : ViewModel() {

    val counterNames: Flow<List<CounterSelectionUiItem>> = editionRepository.editionState.allEditedCountersFlow
        .map { counters ->
            counters.map { counter ->
                CounterSelectionUiItem(
                    counterName = counter.counterName,
                    counterStartingValueDesc = context.getString(
                        R.string.field_counter_selection_desc,
                        counter.defaultValue.toNaturalDisplayString(maxFractionDigits = 2),
                    )
                )
            }.sortedBy { counter -> counter.counterName }
        }
}
