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

import java.util.Locale


/**
 * Languages trained for text detection.
 * @param langCode ISO 639-1 language code.
 */
enum class TrainedTextLanguage(internal val langCode: String) {
    AFRIKAANS("afr"),
    ARABIC("ara"),
    BULGARIAN("bul"),
    CHINESE_SIMPLIFIED("chi_sim"),
    CHINESE_TRADITIONAL("chi_tra"),
    CROATIAN("hrv"),
    CZECH("ces"),
    DANISH("dan"),
    DUTCH("nld"),
    ENGLISH("eng"),
    FINNISH("fin"),
    FRENCH("fra"),
    GERMAN("deu"),
    GREEK("ell"),
    HEBREW("heb"),
    HINDI("hin"),
    HUNGARIAN("hun"),
    INDONESIAN("ind"),
    ITALIAN("ita"),
    JAPANESE("jpn"),
    KOREAN("kor"),
    NORWEGIAN("nor"),
    POLISH("pol"),
    PORTUGUESE("por"),
    ROMANIAN("ron"),
    RUSSIAN("rus"),
    SPANISH("spa"),
    SWEDISH("swe"),
    THAI("tha"),
    TURKISH("tur"),
    UKRAINIAN("ukr"),
    VIETNAMESE("vie");


    companion object {

        /** Get the TrainedTextLanguage corresponding to the locale, if any. */
        fun Locale.getTrainedTextLanguage(): TrainedTextLanguage? =
            when (language) {
                "af" -> AFRIKAANS
                "ar" -> ARABIC
                "bg" -> BULGARIAN
                "zh" -> when (script) {
                    "Hans" -> CHINESE_SIMPLIFIED
                    "Hant" -> CHINESE_TRADITIONAL
                    else -> null // fallback in ambiguous case
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
                else -> null
            }
    }
}

