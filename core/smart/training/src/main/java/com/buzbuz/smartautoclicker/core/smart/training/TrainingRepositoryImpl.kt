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
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextDataState
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


    override val trainedTextLanguages: Flow<TrainedTextData> = textTrainingLocalDataSrc.trainingDataFiles
        .combine(textTrainingRemoteDataSrc.currentDownloadState) { languagesFiles, downloadState ->
            TrainedTextData(
                dataFolder = textTrainingLocalDataSrc.getDataDirectoryPath(),
                languages = getLanguages(languagesFiles, downloadState),
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
    ): Map<TrainedTextLanguage, TrainedTextDataState> =
        buildMap {
            TrainedTextLanguage.entries.forEach { language ->
                when {
                    files.contains(language) ->
                        put(language, TrainedTextDataState.Downloaded)

                    dlState != null && dlState is TextTrainingFileDownload.Downloading && dlState.language == language ->
                        put(language, TrainedTextDataState.Downloading(dlState.progress))

                    dlState != null && dlState is TextTrainingFileDownload.Error && dlState.language == language ->
                        put(language, TrainedTextDataState.DownloadError)

                    else ->
                        put(language, TrainedTextDataState.Absent)
                }
            }
        }
}

private const val TAG = "TrainingRepositoryImpl"