
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.MultiStateButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setup
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldMultiStateBinding


fun IncludeFieldMultiStateBinding.setTitle(titleText: String) {
    titleAndDescription.setTitle(titleText)
}

fun IncludeFieldMultiStateBinding.setupDescriptions(descriptions: List<String>) {
    titleAndDescription.setupDescriptions(descriptions)
}

fun IncludeFieldMultiStateBinding.setDescription(descriptionIndex: Int) {
    titleAndDescription.setDescription(descriptionIndex)
}

fun IncludeFieldMultiStateBinding.setDescription(description: String) {
    titleAndDescription.setDescription(description)
}

fun IncludeFieldMultiStateBinding.setButtonConfig(config: MultiStateButtonConfig) {
    multiStateButton.setup(config)
}

fun IncludeFieldMultiStateBinding.setChecked(checkedId: Int?) {
    multiStateButton.setChecked(checkedId)
}

fun IncludeFieldMultiStateBinding.setOnCheckedListener(listener: ((Int?) -> Unit)?) {
    if (listener == null) multiStateButton.setOnCheckedListener(null)
    else multiStateButton.setOnCheckedListener(listener)
}