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

import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import java.net.URI
import java.net.URL


internal fun TrainedTextLanguage.getTrainedDataUrl(): URL =
    URI("$TRAINING_DATA_URL_BASE${getTrainedDataFileName()}").toURL()

internal fun TrainedTextLanguage.getTrainedDataFileName(): String =
    "$langCode.$TRAINING_DATA_FILE_EXTENSION"

internal const val TRAINING_DATA_FILE_EXTENSION = "traineddata"
private const val TRAINING_DATA_URL_BASE = "https://github.com/tesseract-ocr/tessdata/blob/main/"