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
package com.buzbuz.smartautoclicker.core.smart.training

import android.util.Log

import com.buzbuz.smartautoclicker.core.smart.training.data.TextTrainingFileDownload
import com.buzbuz.smartautoclicker.core.smart.training.data.TextTrainingLocalDataSource
import com.buzbuz.smartautoclicker.core.smart.training.data.TextTrainingRemoteDataSource
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextData
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextDataSyncState
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class TrainingRepositoryImpl @Inject constructor(
    private val textTrainingLocalDataSrc: TextTrainingLocalDataSource,
    private val textTrainingRemoteDataSrc: TextTrainingRemoteDataSource,
) : TrainingRepository {


    override val trainedTextLanguagesSyncState: Flow<Map<TrainedTextLanguage, TrainedTextDataSyncState>> =
        textTrainingLocalDataSrc.trainingDataFiles
            .combine(textTrainingRemoteDataSrc.currentDownloadState) { languagesFiles, downloadState ->
                getLanguages(languagesFiles, downloadState)
            }


    override fun getTrainedTextDataForLanguages(languages: Set<TrainedTextLanguage>): TrainedTextData? {
        val missingTrainingData =
            languages.find { language -> textTrainingLocalDataSrc.getLanguageFile(language) == null } != null
        if (missingTrainingData) {
            Log.e(TAG, "Can't get trained text data for languages $languages")
            return null
        }

        return TrainedTextData(
            textTrainingLocalDataSrc.getDataDirectoryPath(),
            languages.toTesseractLangCodes(),
        )
    }

    override fun downloadTextLanguageDataFile(language: TrainedTextLanguage) {
        val languageFile = textTrainingLocalDataSrc.getLanguageFile(language)
        if (languageFile == null) {
            Log.e(TAG, "Can't download language file, can't create file")
            return
        }

        textTrainingRemoteDataSrc.downloadTrainingDataFile(
            language = language,
            outputFile = languageFile,
            onSucces = { textTrainingLocalDataSrc.refreshTrainingDataFiles() },
        )
    }

    override fun deleteTextLanguageDataFile(language: TrainedTextLanguage) {
        textTrainingLocalDataSrc.deleteTrainingData(language)
    }

    private fun getLanguages(
        files: Map<TrainedTextLanguage, File>,
        dlState: TextTrainingFileDownload?,
    ): Map<TrainedTextLanguage, TrainedTextDataSyncState> =
        buildMap {
            TrainedTextLanguage.entries.forEach { language ->
                when {
                    files.contains(language) ->
                        put(language, TrainedTextDataSyncState.Downloaded)

                    dlState != null && dlState is TextTrainingFileDownload.Downloading && dlState.language == language ->
                        put(language, TrainedTextDataSyncState.Downloading(dlState.progress))

                    dlState != null && dlState is TextTrainingFileDownload.Error && dlState.language == language ->
                        put(language, TrainedTextDataSyncState.DownloadError)

                    else ->
                        put(language, TrainedTextDataSyncState.Absent)
                }
            }
        }

    /**
     * Format the collection of languages into the tesseract language format.
     * This will happen the ISO 639-1 language code with a + between them.
     *
     * Ex: [eng,fra,ita] = "eng+fra+ita"
     */
    private fun Collection<TrainedTextLanguage>.toTesseractLangCodes(): String =
        joinToString(separator = "+")
}

private const val TAG = "TrainingRepositoryImpl"