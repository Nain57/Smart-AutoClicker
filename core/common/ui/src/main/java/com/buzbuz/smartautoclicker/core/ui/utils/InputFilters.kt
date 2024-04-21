/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.utils

import android.text.InputFilter
import android.text.Spanned

import kotlin.reflect.KClass

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
        } catch (_: NumberFormatException) { }
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

/** Input filter for a number between a min and a max. */
class MinMaxInputFilter(
    private val min: Int? = null,
    private val max: Int? = null,
) : InputFilter {

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toInt()

            val isOverMin = min == null || min <= input
            val isBelowMax = max == null || input <= max
            if (isOverMin && isBelowMax) return null
        } catch (_: NumberFormatException) { }
        return ""
    }

}