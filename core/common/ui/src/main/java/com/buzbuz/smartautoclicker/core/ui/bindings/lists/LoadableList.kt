
package com.buzbuz.smartautoclicker.core.ui.bindings.lists

import android.view.View
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding

fun IncludeLoadableListBinding.setEmptyText(@StringRes id: Int, @StringRes secondaryId: Int? = null) {
    emptyText.setText(id)

    if (secondaryId == null) {
        emptySecondary.visibility = View.GONE
    } else {
        emptySecondary.visibility = View.VISIBLE
        emptySecondaryText.setText(secondaryId)
    }
}

fun IncludeLoadableListBinding.updateState(items: Collection<Any>?) {
    when {
        items == null -> {
            loading.visibility = View.VISIBLE
            list.visibility = View.GONE
            empty.visibility = View.GONE
        }
        items.isEmpty() -> {
            loading.visibility = View.GONE
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
        }
        else -> {
            loading.visibility = View.GONE
            list.visibility = View.VISIBLE
            empty.visibility = View.GONE
        }
    }
}