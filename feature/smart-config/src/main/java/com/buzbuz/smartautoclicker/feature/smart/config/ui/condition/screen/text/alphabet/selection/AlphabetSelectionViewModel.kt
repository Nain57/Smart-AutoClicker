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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.OCRModelsRepository
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetDownloadUiState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetSelectionItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.toUiState

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlphabetSelectionViewModel @Inject constructor (
    private val editionRepository: EditionRepository,
    private val ocrModelsRepository: OCRModelsRepository,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val selectedAlphabet: Flow<OCRAlphabet> = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }
        .filterIsInstance<ScreenCondition.Text>()
        .map { textCondition -> textCondition.alphabet }

    private val alphabetItems: Flow<List<AlphabetSelectionItem.Alphabet>> = ocrModelsRepository.recognitionModels
        .combine(selectedAlphabet) { models, selected ->
            models.map { model -> model.toUiState(selected == model.alphabet) }
        }

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    @OptIn(FlowPreview::class)
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    val items: Flow<List<AlphabetSelectionItem>> = alphabetItems
        .map { models ->
            buildList {
                add(AlphabetSelectionItem.Header(R.string.item_alphabet_selection_header))
                addAll(models.sortedByState())
            }
        }

    fun selectModel(alphabet: OCRAlphabet) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Text>()?.let { condition ->
            editionRepository.updateEditedCondition(
                condition.copy(alphabet = alphabet)
            )
        }
    }

    fun downloadModel(alphabet: OCRAlphabet) {
        viewModelScope.launch {
            ocrModelsRepository.downloadRecognitionModel(alphabet)
        }
    }
}

private fun List<AlphabetSelectionItem.Alphabet>.sortedByState() =
    sortedWith(
        compareBy<AlphabetSelectionItem.Alphabet> {
            it.downloadState != AlphabetDownloadUiState.Downloaded
        }.thenBy { it.alphabet.name }
    )