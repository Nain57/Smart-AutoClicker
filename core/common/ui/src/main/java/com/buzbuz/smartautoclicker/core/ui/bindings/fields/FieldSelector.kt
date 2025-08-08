
package com.buzbuz.smartautoclicker.core.ui.bindings.fields

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View

import com.buzbuz.smartautoclicker.core.ui.bindings.ALPHA_DISABLED
import com.buzbuz.smartautoclicker.core.ui.bindings.ALPHA_ENABLED
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.other.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldSelectorBinding


fun IncludeFieldSelectorBinding.setTitle(titleText: String) {
    titleAndDescription.setTitle(titleText)
}

fun IncludeFieldSelectorBinding.setupDescriptions(descriptions: List<String>) {
    titleAndDescription.setupDescriptions(descriptions)
}

fun IncludeFieldSelectorBinding.setDescription(descriptionIndex: Int) {
    titleAndDescription.setDescription(descriptionIndex)
}

fun IncludeFieldSelectorBinding.setDescription(description: String?) {
    titleAndDescription.setDescription(description)
}

fun IncludeFieldSelectorBinding.setIconBitmap(bitmap: Bitmap?) {
    icon.visibility = if (bitmap == null) View.GONE else View.VISIBLE
    icon.setImageBitmap(bitmap)
}

fun IncludeFieldSelectorBinding.setImageDrawable(drawable: Drawable?) {
    icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
    icon.setImageDrawable(drawable)
}

fun IncludeFieldSelectorBinding.setEnabled(isEnabled: Boolean) {
    root.isEnabled = isEnabled
    root.alpha = if (isEnabled) ALPHA_ENABLED else ALPHA_DISABLED
}

fun IncludeFieldSelectorBinding.setOnClickListener(listener: (() -> Unit)?) {
    if (listener == null) root.setOnClickListener(null)
    else root.setOnClickListener { listener() }
}