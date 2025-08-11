
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
            val sanitizedSource = if (source == "-") "-0" else source
            val input = (dest.toString() + sanitizedSource.toString()).toInt()

            val isOverMin = min == null || min <= input
            val isBelowMax = max == null || input <= max
            if (isOverMin && isBelowMax) return null
        } catch (_: NumberFormatException) { }
        return ""
    }

}