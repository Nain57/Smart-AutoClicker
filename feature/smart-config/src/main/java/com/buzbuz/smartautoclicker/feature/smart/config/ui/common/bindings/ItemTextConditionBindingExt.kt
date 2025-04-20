/*
 * Copyright (C) 2025 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeTextConditionCardBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTextCondition

/**
 * Bind the [IncludeTextConditionCardBinding] to a condition.
 */
fun IncludeTextConditionCardBinding.bind(
    uiCondition: UiTextCondition,
    conditionClickedListener: (TextCondition) -> Unit
) {
    root.setOnClickListener { conditionClickedListener.invoke(uiCondition.condition) }

    conditionName.text = uiCondition.name
    conditionShouldBeDetected.setImageResource(uiCondition.shouldBeVisibleIconRes)
    conditionDetectionType.setImageResource(uiCondition.detectionTypeIconRes)
    conditionThreshold.text = uiCondition.thresholdText
    conditionText.text = uiCondition.conditionTextDescription

}