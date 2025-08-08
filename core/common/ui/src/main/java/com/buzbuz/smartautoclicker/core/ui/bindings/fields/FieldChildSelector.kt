
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import android.view.View

import com.buzbuz.smartautoclicker.core.ui.bindings.ALPHA_DISABLED
import com.buzbuz.smartautoclicker.core.ui.bindings.ALPHA_ENABLED
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setIcons
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setTexts
import com.buzbuz.smartautoclicker.core.ui.bindings.setTextOrGone
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldChildSelectorBinding


fun IncludeFieldChildSelectorBinding.setTitle(titleText: String) {
    title.setTextOrGone(titleText)
}

fun IncludeFieldChildSelectorBinding.setChildrenIcons(childrenIcons: List<Int>) {
    childrenContainer.setIcons(childrenIcons)
}

fun IncludeFieldChildSelectorBinding.setChildrenTexts(childrenTexts: List<String>) {
    childrenContainer.apply {
        root.visibility = View.VISIBLE
        setTexts(childrenTexts)
    }
    description.setTextOrGone(null)
}

fun IncludeFieldChildSelectorBinding.setDescription(text: String?) {
    childrenContainer.root.visibility = View.INVISIBLE
    description.setTextOrGone(text)
}

fun IncludeFieldChildSelectorBinding.setEnabled(isEnabled: Boolean) {
    root.isEnabled = isEnabled
    root.alpha = if (isEnabled) ALPHA_ENABLED else ALPHA_DISABLED
}

fun IncludeFieldChildSelectorBinding.setOnClickListener(listener: () -> Unit) {
    root.setOnClickListener { listener() }
}

