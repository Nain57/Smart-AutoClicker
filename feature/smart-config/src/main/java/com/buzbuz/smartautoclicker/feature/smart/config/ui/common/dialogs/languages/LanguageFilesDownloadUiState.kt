/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.languages

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import com.buzbuz.smartautoclicker.feature.smart.config.R

sealed class LanguageFilesUiItem {

    data object Header : LanguageFilesUiItem() {
        @StringRes val text: Int = R.string.item_languages_download_header
    }

    data class Language(
        @StringRes val languageName: Int,
        val downloadState: LanguageFileDownloadUiState,
        val trainedTextLanguage: TrainedTextLanguage,
    ): LanguageFilesUiItem()
}

sealed class LanguageFileDownloadUiState {
    data object NotDownloaded : LanguageFileDownloadUiState()
    data class Downloading(val progressText: String): LanguageFileDownloadUiState()
    data object Downloaded: LanguageFileDownloadUiState()
    data object Error: LanguageFileDownloadUiState()
}