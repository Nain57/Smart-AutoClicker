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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.DialogChoice
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import com.buzbuz.smartautoclicker.core.smart.training.model.toDisplayStringRes

/** Choices for the screen condition type selection dialog. */
data class TextLanguagesChoice(val language: TrainedTextLanguage): DialogChoice(language.toDisplayStringRes())

fun allTextLanguagesChoices(): List<TextLanguagesChoice> = listOf(
    TextLanguagesChoice(TrainedTextLanguage.AFRIKAANS),
    TextLanguagesChoice(TrainedTextLanguage.ARABIC),
    TextLanguagesChoice(TrainedTextLanguage.BULGARIAN),
    TextLanguagesChoice(TrainedTextLanguage.CHINESE_SIMPLIFIED),
    TextLanguagesChoice(TrainedTextLanguage.CHINESE_TRADITIONAL),
    TextLanguagesChoice(TrainedTextLanguage.CROATIAN),
    TextLanguagesChoice(TrainedTextLanguage.CZECH),
    TextLanguagesChoice(TrainedTextLanguage.DANISH),
    TextLanguagesChoice(TrainedTextLanguage.DUTCH),
    TextLanguagesChoice(TrainedTextLanguage.ENGLISH),
    TextLanguagesChoice(TrainedTextLanguage.FINNISH),
    TextLanguagesChoice(TrainedTextLanguage.FRENCH),
    TextLanguagesChoice(TrainedTextLanguage.GERMAN),
    TextLanguagesChoice(TrainedTextLanguage.GREEK),
    TextLanguagesChoice(TrainedTextLanguage.HEBREW),
    TextLanguagesChoice(TrainedTextLanguage.HINDI),
    TextLanguagesChoice(TrainedTextLanguage.HUNGARIAN),
    TextLanguagesChoice(TrainedTextLanguage.INDONESIAN),
    TextLanguagesChoice(TrainedTextLanguage.ITALIAN),
    TextLanguagesChoice(TrainedTextLanguage.JAPANESE),
    TextLanguagesChoice(TrainedTextLanguage.KOREAN),
    TextLanguagesChoice(TrainedTextLanguage.NORWEGIAN),
    TextLanguagesChoice(TrainedTextLanguage.POLISH),
    TextLanguagesChoice(TrainedTextLanguage.PORTUGUESE),
    TextLanguagesChoice(TrainedTextLanguage.ROMANIAN),
    TextLanguagesChoice(TrainedTextLanguage.RUSSIAN),
    TextLanguagesChoice(TrainedTextLanguage.SPANISH),
    TextLanguagesChoice(TrainedTextLanguage.SWEDISH),
    TextLanguagesChoice(TrainedTextLanguage.THAI),
    TextLanguagesChoice(TrainedTextLanguage.TURKISH),
    TextLanguagesChoice(TrainedTextLanguage.UKRAINIAN),
    TextLanguagesChoice(TrainedTextLanguage.VIETNAMESE),
)