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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.config

import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter

sealed class CountersUiState {

    data object Loading : CountersUiState()
    data class Loaded(
        val canBeSaved: Boolean,
        val hasUnsavedModifications: Boolean,
        val counterItems: List<CounterUiItem>,
    ) : CountersUiState()
    data class Replacing(
        val counterItems: List<CounterUiItem>,
    ) : CountersUiState()
    data object Empty : CountersUiState()

}


data class CounterUiItem(
    val counter: Counter,
    val selectedForReplacement: Boolean,
    val counterName: String,
    val counterDesc: String,
    val isExpanded: Boolean,
    val startingValue: Double,
    val setByButtonText: String,
    val setByButtonIsEmpty: Boolean,
    val readByButtonText: String,
    val readByButtonIsEmpty: Boolean,
    val deleteButtonText: String,
    val deleteButtonEnabled: Boolean,
)

