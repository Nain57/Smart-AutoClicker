
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import android.view.View
import com.buzbuz.smartautoclicker.core.base.extensions.getThemeColor
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.utils.setIconTintColor

/**
 * Bind the [ItemImageEventBinding] to an event.
 *
 * @param item the event represented by this view binding.
 * @param canDrag true to show the drag handle, false to hide it.
 * @param itemClickedListener called when the user clicks on the item.
 */
fun ItemImageEventBinding.bind(
    item: UiImageEvent,
    canDrag: Boolean,
    itemClickedListener: (ImageEvent) -> Unit,
) {
    textName.text = item.name
    textConditionsCount.text = item.conditionsCountText

    val actionColor = root.context.getThemeColor(if (item.haveError) R.attr.colorError else R.attr.colorOnSurface)
    textActionsCount.text = item.actionsCountText
    textActionsCount.setTextColor(actionColor)
    imageAction.setIconTintColor(actionColor)

    textEnabled.setText(item.enabledOnStartTextRes)
    iconEnabled.setImageResource(item.enabledOnStartIconRes)

    btnReorder.visibility = if (canDrag) View.VISIBLE else View.GONE

    root.setOnClickListener { itemClickedListener(item.event) }
}