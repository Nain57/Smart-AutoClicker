/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.utils

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.feature.smart.config.R

@StringRes
fun OCRAlphabet.getDisplayNameResId(): Int =
    when (this) {
        OCRAlphabet.ARABIC -> R.string.item_alphabet_name_arabic
        OCRAlphabet.CHINESE_SIMPLIFIED -> R.string.item_alphabet_name_chinese_simplified
        OCRAlphabet.CHINESE_TRADITIONAL -> R.string.item_alphabet_name_chinese_traditional
        OCRAlphabet.CYRILLIC -> R.string.item_alphabet_name_cyrillic
        OCRAlphabet.DEVANAGARI -> R.string.item_alphabet_name_devanagari
        OCRAlphabet.JAPANESE -> R.string.item_alphabet_name_japanese
        OCRAlphabet.KANNADA -> R.string.item_alphabet_name_kannada
        OCRAlphabet.KOREAN -> R.string.item_alphabet_name_korean
        OCRAlphabet.LATIN -> R.string.item_alphabet_name_latin
        OCRAlphabet.TAMIL -> R.string.item_alphabet_name_tamil
        OCRAlphabet.TELUGU -> R.string.item_alphabet_name_telugu
    }

@StringRes
fun OCRAlphabet.getDescriptionResId(): Int =
    when (this) {
        OCRAlphabet.ARABIC -> R.string.item_alphabet_name_arabic_desc
        OCRAlphabet.CHINESE_SIMPLIFIED -> R.string.item_alphabet_name_chinese_simplified_desc
        OCRAlphabet.CHINESE_TRADITIONAL -> R.string.item_alphabet_name_chinese_traditional_desc
        OCRAlphabet.CYRILLIC -> R.string.item_alphabet_name_cyrillic_desc
        OCRAlphabet.DEVANAGARI -> R.string.item_alphabet_name_devanagari_desc
        OCRAlphabet.JAPANESE -> R.string.item_alphabet_name_japanese_desc
        OCRAlphabet.KANNADA -> R.string.item_alphabet_name_kannada_desc
        OCRAlphabet.KOREAN -> R.string.item_alphabet_name_korean_desc
        OCRAlphabet.LATIN -> R.string.item_alphabet_name_latin_desc
        OCRAlphabet.TAMIL -> R.string.item_alphabet_name_tamil_desc
        OCRAlphabet.TELUGU -> R.string.item_alphabet_name_telugu_desc
    }