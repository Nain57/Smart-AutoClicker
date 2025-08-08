
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import com.buzbuz.smartautoclicker.core.ui.bindings.other.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldSwitchBinding


fun IncludeFieldSwitchBinding.setTitle(titleText: String) {
    titleAndDescription.setTitle(titleText)
}

fun IncludeFieldSwitchBinding.setupDescriptions(descriptions: List<String>) {
    titleAndDescription.setupDescriptions(descriptions)
}

fun IncludeFieldSwitchBinding.setDescription(descriptionIndex: Int) {
    titleAndDescription.setDescription(descriptionIndex)
}

fun IncludeFieldSwitchBinding.setDescription(description: String) {
    titleAndDescription.setDescription(description)
}

fun IncludeFieldSwitchBinding.setChecked(isChecked: Boolean) {
    toggleSwitch.isChecked = isChecked
}

fun IncludeFieldSwitchBinding.setOnClickListener(listener: (() -> Unit)?) {
    if (listener == null) toggleSwitch.setOnClickListener(null)
    else toggleSwitch.setOnClickListener { listener() }
}