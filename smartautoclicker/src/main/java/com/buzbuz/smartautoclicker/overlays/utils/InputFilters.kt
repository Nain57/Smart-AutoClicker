/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.utils

import android.text.InputFilter
import android.text.Spanned

import kotlin.reflect.KClass

/** Input filter for an Action duration. */
class DurationInputFilter : InputFilter {

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            if (Integer.parseInt(dest.toString() + source.toString()) > 0) return null
        } catch (nfe: NumberFormatException) { }
        return ""
    }

}

/** Input filter for a number. Ensure the value is within bounds. */
class NumberInputFilter<T : Number>(private val type: KClass<T>): InputFilter {

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val newValue = "${dest!!.subSequence(0, dstart)}" +
                "${source!!.subSequence(start, end)}" +
                "${dest.subSequence(dend, dest.length)}"

        try {
            if (newValue == "-" || isInRange(newValue)) return null
        } catch (nfe: NumberFormatException) { }
        return ""
    }

    /** Check if the provided value is in the correct range. */
    private fun isInRange(value: String): Boolean = try {
        when (type) {
            Byte::class -> value.toLong() in Byte.MIN_VALUE..Byte.MAX_VALUE
            Short::class -> value.toLong() in Short.MIN_VALUE..Short.MAX_VALUE
            Int::class -> value.toLong() in Int.MIN_VALUE..Int.MAX_VALUE
            Long::class -> value.toLong() in Long.MIN_VALUE..Long.MAX_VALUE
            Float::class -> value.toDouble() in -Float.MAX_VALUE..Float.MAX_VALUE
            Double::class -> value.toDouble() in -Double.MAX_VALUE..Double.MAX_VALUE
            else -> throw IllegalArgumentException("Invalid type.")
        }
    } catch (nfe: NumberFormatException) { false }
}