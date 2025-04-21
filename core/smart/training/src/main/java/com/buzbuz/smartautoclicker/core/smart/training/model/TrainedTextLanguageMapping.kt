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
package com.buzbuz.smartautoclicker.core.smart.training.model

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.smart.training.R
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage.*
import java.util.Locale


/** Get the TrainedTextLanguage corresponding to the locale, if any. */
fun Locale.toTrainedTextLanguage(): TrainedTextLanguage =
    when (language) {
        "af" -> AFRIKAANS
        "ar" -> ARABIC
        "bg" -> BULGARIAN
        "zh" -> when (script) {
            "Hans" -> CHINESE_SIMPLIFIED
            "Hant" -> CHINESE_TRADITIONAL
            else -> CHINESE_SIMPLIFIED // fallback in ambiguous case
        }
        "hr" -> CROATIAN
        "cs" -> CZECH
        "da" -> DANISH
        "nl" -> DUTCH
        "en" -> ENGLISH
        "fi" -> FINNISH
        "fr" -> FRENCH
        "de" -> GERMAN
        "el" -> GREEK
        "he", "iw" -> HEBREW
        "hi" -> HINDI
        "hu" -> HUNGARIAN
        "id", "in" -> INDONESIAN
        "it" -> ITALIAN
        "ja" -> JAPANESE
        "ko" -> KOREAN
        "no", "nb", "nn" -> NORWEGIAN
        "pl" -> POLISH
        "pt" -> PORTUGUESE
        "ro" -> ROMANIAN
        "ru" -> RUSSIAN
        "es" -> SPANISH
        "sv" -> SWEDISH
        "th" -> THAI
        "tr" -> TURKISH
        "uk" -> UKRAINIAN
        "vi" -> VIETNAMESE
        else -> ENGLISH
    }

@StringRes
fun TrainedTextLanguage.toDisplayStringRes(): Int =
    when (this) {
        AFRIKAANS -> R.string.language_name_afrikaans
        ARABIC -> R.string.language_name_arabic
        BULGARIAN -> R.string.language_name_bulgarian
        CHINESE_SIMPLIFIED -> R.string.language_name_chinese_simplified
        CHINESE_TRADITIONAL -> R.string.language_name_chinese_traditional
        CROATIAN -> R.string.language_name_croatian
        CZECH -> R.string.language_name_czech
        DANISH -> R.string.language_name_danish
        DUTCH -> R.string.language_name_dutch
        ENGLISH -> R.string.language_name_english
        FINNISH -> R.string.language_name_finnish
        FRENCH -> R.string.language_name_french
        GERMAN -> R.string.language_name_german
        GREEK -> R.string.language_name_greek
        HEBREW -> R.string.language_name_hebrew
        HINDI -> R.string.language_name_hindi
        HUNGARIAN -> R.string.language_name_hungarian
        INDONESIAN -> R.string.language_name_indonesian
        ITALIAN -> R.string.language_name_italian
        JAPANESE -> R.string.language_name_japanese
        KOREAN -> R.string.language_name_korean
        NORWEGIAN -> R.string.language_name_norwegian
        POLISH -> R.string.language_name_polish
        PORTUGUESE -> R.string.language_name_portuguese
        ROMANIAN -> R.string.language_name_romanian
        RUSSIAN -> R.string.language_name_russian
        SPANISH -> R.string.language_name_spanish
        SWEDISH -> R.string.language_name_swedish
        THAI -> R.string.language_name_thai
        TURKISH -> R.string.language_name_turkish
        UKRAINIAN -> R.string.language_name_ukrainian
        VIETNAMESE -> R.string.language_name_vietnamese
    }