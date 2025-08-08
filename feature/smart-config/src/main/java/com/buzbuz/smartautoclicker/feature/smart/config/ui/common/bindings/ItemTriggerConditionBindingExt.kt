
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerConditionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTriggerCondition

/**
 * Bind this view holder as a condition item.
 *
 * @param uiCondition the condition to be represented by this item.
 * @param conditionClickedListener listener notified upon user click on this item.
 */
fun ItemTriggerConditionBinding.bind(
    uiCondition: UiTriggerCondition,
    conditionClickedListener: (TriggerCondition) -> Unit,
) {
    conditionName.text = uiCondition.name
    conditionDetails.text = uiCondition.description
    conditionTypeIcon.setImageResource(uiCondition.iconRes)
    root.setOnClickListener { conditionClickedListener(uiCondition.condition) }
}
