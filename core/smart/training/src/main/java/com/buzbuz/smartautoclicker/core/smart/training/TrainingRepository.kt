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

import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextData
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextDataSyncState
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import kotlinx.coroutines.flow.Flow

/** Handles the training data for the different detection algorithms. */
interface TrainingRepository {

    /** State of the training data for the text languages. */
    val trainedTextLanguagesSyncState: Flow<Map<TrainedTextLanguage, TrainedTextDataSyncState>>

    fun getTrainedTextDataForLanguages(languages: Set<TrainedTextLanguage>): TrainedTextData?
    fun downloadTextLanguageDataFile(language: TrainedTextLanguage)
    fun deleteTextLanguageDataFile(language: TrainedTextLanguage)
}