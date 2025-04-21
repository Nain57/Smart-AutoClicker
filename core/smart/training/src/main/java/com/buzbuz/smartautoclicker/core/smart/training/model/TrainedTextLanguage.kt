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
}

