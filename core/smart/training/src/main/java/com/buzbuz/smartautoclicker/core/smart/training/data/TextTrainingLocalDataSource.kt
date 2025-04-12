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
package com.buzbuz.smartautoclicker.core.smart.training.data

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.extensions.safeDelete
import com.buzbuz.smartautoclicker.core.base.extensions.safeMkDirs
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class TextTrainingLocalDataSource @Inject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext context: Context,
) {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val directory: File = File(context.filesDir, TRAINING_DATA_DIR)
    private val refresh: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)


    val trainingDataFiles: Flow<Map<TrainedTextLanguage, File>> = refresh
        .onStart { emit(Unit) }
        .map { listLanguagesFiles() }
        .flowOn(ioDispatcher)


    fun refreshTrainingDataFiles() {
        coroutineScopeIo.launch {
            Log.i(TAG, "Refreshing training data folder content")
            refresh.emit(Unit)
        }
    }

    fun getDataDirectoryPath(): String =
        directory.path

    fun getLanguageFile(language: TrainedTextLanguage): File? {
        if (!directory.exists()) return null
        return File(directory, language.getTrainedDataFileName())
    }

    fun deleteTrainingData(language: TrainedTextLanguage) {
        coroutineScopeIo.launch {
            Log.i(TAG, "Deleting $language training data")
            File(directory, language.getTrainedDataFileName()).safeDelete()
            refreshTrainingDataFiles()
        }
    }

    private fun listLanguagesFiles(): Map<TrainedTextLanguage, File> {
        if (!directory.safeMkDirs()) {
            Log.e(TAG, "Can't create text training data directory: ${directory.path}")
            return emptyMap()
        }

        return buildMap {
            directory.listFiles()?.forEach { file ->
                val fileNameParts = file.name.split(".")

                if (fileNameParts[1] != TRAINING_DATA_FILE_EXTENSION) return@forEach
                TrainedTextLanguage.entries.find { language -> fileNameParts[0] == language.langCode }?.let { language ->
                    put(language, file)
                }
            }
        }
    }
}

private const val TRAINING_DATA_DIR = "TextTrainingData"
private const val TAG = "TextTrainingDataSource"