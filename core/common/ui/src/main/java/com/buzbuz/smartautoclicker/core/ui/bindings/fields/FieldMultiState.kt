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

import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.MultiStateButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setup
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setTextOrGone
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldMultiStateBinding


fun IncludeFieldMultiStateBinding.setTitle(titleText: String) {
    title.setTextOrGone(titleText)
}

fun IncludeFieldMultiStateBinding.setDescription(descriptionText: String?) {
    description.setTextOrGone(descriptionText)
}

fun IncludeFieldMultiStateBinding.setButtonConfig(config: MultiStateButtonConfig) {
    multiStateButton.setup(config)
}

fun IncludeFieldMultiStateBinding.setChecked(checkedId: Int) {
    multiStateButton.setChecked(checkedId)
}

fun IncludeFieldMultiStateBinding.setOnCheckedListener(listener: ((Int?) -> Unit)?) {
    if (listener == null) multiStateButton.setOnCheckedListener(null)
    else multiStateButton.setOnCheckedListener(listener)
}