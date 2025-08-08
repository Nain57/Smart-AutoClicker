
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