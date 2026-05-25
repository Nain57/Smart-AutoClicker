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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.feature.smart.config.R


sealed class AlphabetSelectionItem {

    data object Header : AlphabetSelectionItem() {
        @field:StringRes val text: Int = R.string.item_alphabet_selection_header
    }

    data class Alphabet(
        @field:StringRes val alphabetName: Int,
        @field:StringRes val alphabetDesc: Int,
        val downloadState: AlphabetDownloadUiState,
        val alphabet: OCRAlphabet,
        val selected: Boolean,
    ): AlphabetSelectionItem()
}

sealed class AlphabetDownloadUiState {
    data object NotDownloaded : AlphabetDownloadUiState()
    data class Downloading(val progressText: String): AlphabetDownloadUiState()
    data object Downloaded: AlphabetDownloadUiState()
    data object Error: AlphabetDownloadUiState()
}