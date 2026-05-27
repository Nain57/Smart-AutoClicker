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
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModel
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModelState
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getDescriptionResId
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getDisplayNameResId


sealed class AlphabetSelectionItem {

    data class Header(
        @field:StringRes val text: Int,
    ) : AlphabetSelectionItem()

    data class Alphabet(
        @field:StringRes val alphabetName: Int,
        @field:StringRes val alphabetDesc: Int,
        val downloadState: AlphabetDownloadUiState,
        val alphabet: OCRAlphabet,
        val selected: Boolean,
        val selectableWhenInstalled: Boolean = true,
    ): AlphabetSelectionItem()
}

sealed class AlphabetDownloadUiState {
    data object NotDownloaded : AlphabetDownloadUiState()
    data class Downloading(val progressText: String): AlphabetDownloadUiState()
    data object Downloaded: AlphabetDownloadUiState()
    data object Error: AlphabetDownloadUiState()
}


internal fun OCRModel.Recognition.toUiState(selected: Boolean, selectable: Boolean = true): AlphabetSelectionItem.Alphabet =
    AlphabetSelectionItem.Alphabet(
        alphabet = alphabet,
        alphabetName = alphabet.getDisplayNameResId(),
        alphabetDesc = alphabet.getDescriptionResId(),
        downloadState = state.toDownloadState(),
        selected = selected,
        selectableWhenInstalled = selectable,
    )

internal fun OCRModelState.toDownloadState(): AlphabetDownloadUiState =
    when (this) {
        OCRModelState.Downloadable -> AlphabetDownloadUiState.NotDownloaded
        is OCRModelState.Downloading -> AlphabetDownloadUiState.Downloading("$progress%")
        is OCRModelState.Installed -> AlphabetDownloadUiState.Downloaded
    }