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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.required

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.OCRModelsRepository
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.alphabet.AreRequiredAlphabetModelsInstalledUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.alphabet.GetRequiredAlphabetModelsUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetSelectionItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.toUiState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
class RequiredAlphabetViewModel @Inject constructor (
    smartEngine: SmartProcessingRepository,
    getRequiredAlphabetModelsUseCase: GetRequiredAlphabetModelsUseCase,
    areRequiredAlphabetModelsInstalledUseCase: AreRequiredAlphabetModelsInstalledUseCase,
    private val ocrModelsRepository: OCRModelsRepository,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val requiredModels: Flow<List<AlphabetSelectionItem.Alphabet>> = smartEngine.scenarioId
        .flatMapLatest { identifier ->
            identifier?.databaseId?.let { dbId -> getRequiredAlphabetModelsUseCase(dbId) }
                ?: flowOf(emptyList())
        }
        .map { required ->
            required.map { model -> model.toUiState(selected = false, selectable = false) }
        }

    val canContinue: Flow<Boolean> = smartEngine.scenarioId
        .flatMapLatest { identifier ->
            identifier?.databaseId?.let { dbId -> areRequiredAlphabetModelsInstalledUseCase(dbId) }
                ?: flowOf(true)
        }

    val items: Flow<List<AlphabetSelectionItem>> = requiredModels
        .map { models ->
            buildList {
                if (models.isNotEmpty()) {
                    add(AlphabetSelectionItem.Header(R.string.item_alphabet_required_header))
                    addAll(models)
                }
            }
        }

    fun downloadModel(alphabet: OCRAlphabet) {
        viewModelScope.launch {
            ocrModelsRepository.downloadRecognitionModel(alphabet)
        }
    }
}
