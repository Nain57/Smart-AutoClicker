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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings

import android.text.Editable
import android.text.TextWatcher
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setNumericValue
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldTextInputWithCheckboxBinding
import kotlin.text.indexOf

fun IncludeFieldTextInputWithCheckboxBinding.setPositionValue(value: String) {
    setNumericValue(value, value.isCounterValue())
}

fun IncludeFieldTextInputWithCheckboxBinding.enableClearCounterOnType() {
    textField.addTextChangedListener(object : TextWatcher {
        private var hadCounterBefore = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            hadCounterBefore = s?.startsWith(COUNTER_PREFIX) == true
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (!hadCounterBefore || s == null) return
            hadCounterBefore = false

            val start = s.indexOf(COUNTER_PREFIX)
            val end = s.indexOf(COUNTER_SUFFIX)
            if (start in 0..< end) s.delete(start, end + 1)
        }
    })
}

private fun CharSequence.isCounterValue(): Boolean =
    length > 2 && first() == COUNTER_PREFIX && last() == COUNTER_SUFFIX

private const val COUNTER_PREFIX = '{'
private const val COUNTER_SUFFIX = '}'