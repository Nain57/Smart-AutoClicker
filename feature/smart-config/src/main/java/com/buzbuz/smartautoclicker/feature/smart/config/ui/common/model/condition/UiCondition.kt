
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition

sealed class UiCondition {
    abstract val condition: Condition
    abstract val name: String
    abstract val haveError: Boolean
}