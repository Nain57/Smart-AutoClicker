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

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.smart.training.TrainingRepository
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextDataSyncState
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import com.buzbuz.smartautoclicker.core.smart.training.model.toDisplayStringRes

import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
class LanguageFilesDownloadViewModel @Inject constructor(
    private val repository: IRepository,
    private val trainingRepository: TrainingRepository,
) : ViewModel() {

    private val scenarioDbId: MutableStateFlow<Long?> = MutableStateFlow(null)

    private val scenarioLanguageItems: Flow<List<LanguageFilesUiItem.Language>> = scenarioDbId
        .flatMapLatest { id -> id?.let(repository::getEventsFlow) ?: emptyFlow() }
        .map { events -> events.getAllTextLanguages() }
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


    fun setScenarioId(dbId: Long) {
        scenarioDbId.update { dbId }
    }

    fun downloadLanguageFile(item: LanguageFilesUiItem.Language) {
        trainingRepository.downloadTextLanguageDataFile(item.trainedTextLanguage)
    }

    fun cancelDownload() {
        trainingRepository.cancelTextLanguageDataFileDownload()
    }
}

private fun List<Event>.getAllTextLanguages() : Set<TrainedTextLanguage> =
    buildSet {
        this@getAllTextLanguages.forEach { event ->
            event.conditions.forEach { condition ->
                if (condition is TextCondition) add(condition.textLanguage)
            }
        }
    }

private fun Set<TrainedTextLanguage>.toUiItems(
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