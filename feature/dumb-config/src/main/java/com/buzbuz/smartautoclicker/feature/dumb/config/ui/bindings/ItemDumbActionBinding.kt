
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.bindings

import android.util.TypedValue
import android.view.View

import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbActionBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy.DumbActionDetails

fun ItemDumbActionBinding.onBind(
    details: DumbActionDetails,
    showHandles: Boolean,
    actionClickedListener: (DumbActionDetails) -> Unit,
) {
    root.setOnClickListener { actionClickedListener(details) }

    btnReorder.visibility = if (showHandles) View.VISIBLE else View.GONE
    actionName.visibility = View.VISIBLE
    actionTypeIcon.setImageResource(details.icon)
    actionName.text = details.name
    actionDuration.apply {
        text = details.detailsText

        val typedValue = TypedValue()
        val actionColorAttr = if (details.haveError) R.attr.colorError else R.attr.colorOnSurfaceVariant
        root.context.theme.resolveAttribute(actionColorAttr, typedValue, true)
        setTextColor(typedValue.data)
    }

    actionRepeat.apply {
        if (details.repeatCountText != null) {
            text = details.repeatCountText
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }
}