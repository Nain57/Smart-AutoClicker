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
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.DualStateButtonTextConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setup
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldDualStateTextBinding


fun IncludeFieldDualStateTextBinding.setTitle(titleText: String) {
    titleAndDescription.setTitle(titleText)
}

fun IncludeFieldDualStateTextBinding.setupDescriptions(descriptions: List<String>) {
    titleAndDescription.setupDescriptions(descriptions)
}

fun IncludeFieldDualStateTextBinding.setDescription(descriptionIndex: Int) {
    titleAndDescription.setDescription(descriptionIndex)
}

fun IncludeFieldDualStateTextBinding.setDescription(description: String) {
    titleAndDescription.setDescription(description)
}

fun IncludeFieldDualStateTextBinding.setButtonConfig(config: DualStateButtonTextConfig) {
    dualStateButton.setup(config)
}

fun IncludeFieldDualStateTextBinding.setChecked(checkedId: Int) {
    dualStateButton.setChecked(checkedId)
}

fun IncludeFieldDualStateTextBinding.setOnCheckedListener(listener: ((Int?) -> Unit)?) {
    if (listener == null) dualStateButton.setOnCheckedListener(null)
    else dualStateButton.setOnCheckedListener(listener)
}