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
import com.buzbuz.smartautoclicker.feature.smart.config.R

/** Choices for the screen condition type selection dialog. */
sealed class TextLanguagesChoice(title: Int, val language: TrainedTextLanguage): DialogChoice(title) {
    data object Afrikaans : TextLanguagesChoice(R.string.language_name_afrikaans, TrainedTextLanguage.AFRIKAANS)
    data object Arabic : TextLanguagesChoice(R.string.language_name_arabic, TrainedTextLanguage.ARABIC)
    data object Bulgarian : TextLanguagesChoice(R.string.language_name_bulgarian, TrainedTextLanguage.BULGARIAN)
    data object ChineseSimplified : TextLanguagesChoice(R.string.language_name_chinese_simplified, TrainedTextLanguage.CHINESE_SIMPLIFIED)
    data object ChinesTraditional : TextLanguagesChoice(R.string.language_name_chinese_traditional, TrainedTextLanguage.CHINESE_TRADITIONAL)
    data object Croatian : TextLanguagesChoice(R.string.language_name_croatian, TrainedTextLanguage.CROATIAN)
    data object Czech : TextLanguagesChoice(R.string.language_name_czech, TrainedTextLanguage.CZECH)
    data object Danish : TextLanguagesChoice(R.string.language_name_danish, TrainedTextLanguage.DANISH)
    data object Dutch : TextLanguagesChoice(R.string.language_name_dutch, TrainedTextLanguage.DUTCH)
    data object English : TextLanguagesChoice(R.string.language_name_english, TrainedTextLanguage.ENGLISH)
    data object Finnish : TextLanguagesChoice(R.string.language_name_finnish, TrainedTextLanguage.FINNISH)
    data object French : TextLanguagesChoice(R.string.language_name_french, TrainedTextLanguage.FRENCH)
    data object German : TextLanguagesChoice(R.string.language_name_german, TrainedTextLanguage.GERMAN)
    data object Greek : TextLanguagesChoice(R.string.language_name_greek, TrainedTextLanguage.GREEK)
    data object Hebrew : TextLanguagesChoice(R.string.language_name_hebrew, TrainedTextLanguage.HEBREW)
    data object Hindi : TextLanguagesChoice(R.string.language_name_hindi, TrainedTextLanguage.HINDI)
    data object Hungarian : TextLanguagesChoice(R.string.language_name_hungarian, TrainedTextLanguage.HUNGARIAN)
    data object Indonesian : TextLanguagesChoice(R.string.language_name_indonesian, TrainedTextLanguage.INDONESIAN)
    data object Italian : TextLanguagesChoice(R.string.language_name_italian, TrainedTextLanguage.ITALIAN)
    data object Japanese : TextLanguagesChoice(R.string.language_name_japanese, TrainedTextLanguage.JAPANESE)
    data object Korean : TextLanguagesChoice(R.string.language_name_korean, TrainedTextLanguage.KOREAN)
    data object Norwegian : TextLanguagesChoice(R.string.language_name_norwegian, TrainedTextLanguage.NORWEGIAN)
    data object Polish : TextLanguagesChoice(R.string.language_name_polish, TrainedTextLanguage.POLISH)
    data object Portuguese : TextLanguagesChoice(R.string.language_name_portuguese, TrainedTextLanguage.PORTUGUESE)
    data object Romanian : TextLanguagesChoice(R.string.language_name_romanian, TrainedTextLanguage.ROMANIAN)
    data object Russian : TextLanguagesChoice(R.string.language_name_russian, TrainedTextLanguage.RUSSIAN)
    data object Spanish : TextLanguagesChoice(R.string.language_name_spanish, TrainedTextLanguage.SPANISH)
    data object Swedish : TextLanguagesChoice(R.string.language_name_swedish, TrainedTextLanguage.SWEDISH)
    data object Thai : TextLanguagesChoice(R.string.language_name_thai, TrainedTextLanguage.THAI)
    data object Turkish : TextLanguagesChoice(R.string.language_name_turkish, TrainedTextLanguage.TURKISH)
    data object Ukrainian : TextLanguagesChoice(R.string.language_name_ukrainian, TrainedTextLanguage.UKRAINIAN)
    data object Vietnamese : TextLanguagesChoice(R.string.language_name_vietnamese, TrainedTextLanguage.VIETNAMESE)
}

fun TrainedTextLanguage.toTextLanguageChoice(): TextLanguagesChoice =
    when (this) {
        TrainedTextLanguage.AFRIKAANS -> TextLanguagesChoice.Afrikaans
        TrainedTextLanguage.ARABIC -> TextLanguagesChoice.Arabic
        TrainedTextLanguage.BULGARIAN -> TextLanguagesChoice.Bulgarian
        TrainedTextLanguage.CHINESE_SIMPLIFIED -> TextLanguagesChoice.ChineseSimplified
        TrainedTextLanguage.CHINESE_TRADITIONAL -> TextLanguagesChoice.ChinesTraditional
        TrainedTextLanguage.CROATIAN -> TextLanguagesChoice.Croatian
        TrainedTextLanguage.CZECH -> TextLanguagesChoice.Czech
        TrainedTextLanguage.DANISH -> TextLanguagesChoice.Danish
        TrainedTextLanguage.DUTCH -> TextLanguagesChoice.Dutch
        TrainedTextLanguage.ENGLISH -> TextLanguagesChoice.English
        TrainedTextLanguage.FINNISH -> TextLanguagesChoice.Finnish
        TrainedTextLanguage.FRENCH -> TextLanguagesChoice.French
        TrainedTextLanguage.GERMAN -> TextLanguagesChoice.German
        TrainedTextLanguage.GREEK -> TextLanguagesChoice.Greek
        TrainedTextLanguage.HEBREW -> TextLanguagesChoice.Hebrew
        TrainedTextLanguage.HINDI -> TextLanguagesChoice.Hindi
        TrainedTextLanguage.HUNGARIAN -> TextLanguagesChoice.Hungarian
        TrainedTextLanguage.INDONESIAN -> TextLanguagesChoice.Indonesian
        TrainedTextLanguage.ITALIAN -> TextLanguagesChoice.Italian
        TrainedTextLanguage.JAPANESE -> TextLanguagesChoice.Japanese
        TrainedTextLanguage.KOREAN -> TextLanguagesChoice.Korean
        TrainedTextLanguage.NORWEGIAN -> TextLanguagesChoice.Norwegian
        TrainedTextLanguage.POLISH -> TextLanguagesChoice.Polish
        TrainedTextLanguage.PORTUGUESE -> TextLanguagesChoice.Portuguese
        TrainedTextLanguage.ROMANIAN -> TextLanguagesChoice.Romanian
        TrainedTextLanguage.RUSSIAN -> TextLanguagesChoice.Russian
        TrainedTextLanguage.SPANISH -> TextLanguagesChoice.Spanish
        TrainedTextLanguage.SWEDISH -> TextLanguagesChoice.Swedish
        TrainedTextLanguage.THAI -> TextLanguagesChoice.Thai
        TrainedTextLanguage.TURKISH -> TextLanguagesChoice.Turkish
        TrainedTextLanguage.UKRAINIAN -> TextLanguagesChoice.Ukrainian
        TrainedTextLanguage.VIETNAMESE -> TextLanguagesChoice.Vietnamese
    }


fun allTextLanguagesChoices(): List<TextLanguagesChoice> = listOf(
    TextLanguagesChoice.Afrikaans,
    TextLanguagesChoice.Arabic,
    TextLanguagesChoice.Bulgarian,
    TextLanguagesChoice.ChineseSimplified,
    TextLanguagesChoice.ChinesTraditional,
    TextLanguagesChoice.Croatian,
    TextLanguagesChoice.Czech,
    TextLanguagesChoice.Danish,
    TextLanguagesChoice.Dutch,
    TextLanguagesChoice.English,
    TextLanguagesChoice.Finnish,
    TextLanguagesChoice.French,
    TextLanguagesChoice.German,
    TextLanguagesChoice.Greek,
    TextLanguagesChoice.Hebrew,
    TextLanguagesChoice.Hindi,
    TextLanguagesChoice.Hungarian,
    TextLanguagesChoice.Indonesian,
    TextLanguagesChoice.Italian,
    TextLanguagesChoice.Japanese,
    TextLanguagesChoice.Korean,
    TextLanguagesChoice.Norwegian,
    TextLanguagesChoice.Polish,
    TextLanguagesChoice.Portuguese,
    TextLanguagesChoice.Romanian,
    TextLanguagesChoice.Russian,
    TextLanguagesChoice.Spanish,
    TextLanguagesChoice.Swedish,
    TextLanguagesChoice.Thai,
    TextLanguagesChoice.Turkish,
    TextLanguagesChoice.Ukrainian,
    TextLanguagesChoice.Vietnamese,
)