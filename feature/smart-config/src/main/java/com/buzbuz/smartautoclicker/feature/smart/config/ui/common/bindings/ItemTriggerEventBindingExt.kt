
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import com.buzbuz.smartautoclicker.core.base.extensions.getThemeColor
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiTriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.utils.setIconTintColor

/**
 * Bind this view holder to an event.
 *
 * @param item the item providing the binding data.
 * @param itemClickedListener listener called when an event is clicked.
 */
fun ItemTriggerEventBinding.bind(item: UiTriggerEvent, itemClickedListener: (TriggerEvent) -> Unit) {
    textName.text = item.name
    textConditionsCount.text = item.conditionsCountText

    val actionColor = root.context.getThemeColor(if (item.haveError) R.attr.colorError else R.attr.colorOnSurface)
    textActionsCount.text = item.actionsCountText
    textActionsCount.setTextColor(actionColor)
    imageAction.setIconTintColor(actionColor)

    textEnabled.setText(item.enabledOnStartTextRes)
    iconEnabled.setImageResource(item.enabledOnStartIconRes)

    root.setOnClickListener { itemClickedListener(item.event) }
}