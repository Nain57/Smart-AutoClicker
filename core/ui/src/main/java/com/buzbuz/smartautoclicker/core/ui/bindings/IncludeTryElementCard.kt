/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.bindings

import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeTryElementCardBinding

fun IncludeTryElementCardBinding.setElementTypeName(elementName: String) {
    testTitleText.text = testTitleText.context.getString(R.string.item_title_try_element, elementName)
}

fun IncludeTryElementCardBinding.setOnClickListener(listener: () -> Unit) {
    buttonTest.setOnClickListener { listener() }
}

fun IncludeTryElementCardBinding.setEnabledState(enabledState: Boolean) {
    buttonTest.apply {
        alpha = if (enabledState) ENABLED_ITEM_ALPHA else DISABLED_ITEM_ALPHA
        isEnabled = enabledState
    }
}

private const val ENABLED_ITEM_ALPHA = 1f
private const val DISABLED_ITEM_ALPHA = 0.5f