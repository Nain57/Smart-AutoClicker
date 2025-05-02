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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.smart.training.TrainingRepository
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextDataSyncState
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import com.buzbuz.smartautoclicker.core.smart.training.model.toDisplayStringRes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import javax.inject.Inject


class LanguageFilesDownloadViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
) : ViewModel() {

    private val languagesToDl: MutableStateFlow<List<TrainedTextLanguage>> = MutableStateFlow(emptyList())

    private val scenarioLanguageItems: Flow<List<LanguageFilesUiItem.Language>> = languagesToDl
        .combine(trainingRepository.trainedTextLanguagesSyncState) { scenarioLanguages, syncStates ->
            scenarioLanguages.toUiItems(syncStates)
        }

    val areAllLanguagesDownloaded: Flow<Boolean> = scenarioLanguageItems
        .map { it.find { item -> item.downloadState !is LanguageFileDownloadUiState.Downloaded } == null }

    val areItemsDownloading: StateFlow<Boolean> = scenarioLanguageItems
        .map { it.find { item -> item.downloadState is LanguageFileDownloadUiState.Downloading } != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiItems: Flow<List<LanguageFilesUiItem>> = scenarioLanguageItems
        .map { languageItems ->
            buildList {
                add(LanguageFilesUiItem.Header)
                addAll(languageItems)
            }
        }


    fun setLanguagesToDownload(languages: List<TrainedTextLanguage>) {
        languagesToDl.update { languages }
    }

    fun downloadLanguageFile(item: LanguageFilesUiItem.Language) {
        trainingRepository.downloadTextLanguageDataFile(item.trainedTextLanguage)
    }

    fun cancelDownload() {
        trainingRepository.cancelTextLanguageDataFileDownload()
    }
}

private fun List<TrainedTextLanguage>.toUiItems(
    states: Map<TrainedTextLanguage, TrainedTextDataSyncState>,
): List<LanguageFilesUiItem.Language> =
    mapNotNull { language ->
        states[language]?.toUiState()?.let { downloadState ->
            LanguageFilesUiItem.Language(
                languageName = language.toDisplayStringRes(),
                trainedTextLanguage = language,
                downloadState = downloadState,
            )
        }
    }


private fun TrainedTextDataSyncState.toUiState(): LanguageFileDownloadUiState =
    when (this) {
        TrainedTextDataSyncState.Absent -> LanguageFileDownloadUiState.NotDownloaded
        TrainedTextDataSyncState.DownloadError -> LanguageFileDownloadUiState.Error
        TrainedTextDataSyncState.Downloaded -> LanguageFileDownloadUiState.Downloaded
        is TrainedTextDataSyncState.Downloading -> LanguageFileDownloadUiState.Downloading("$progress%")
    }