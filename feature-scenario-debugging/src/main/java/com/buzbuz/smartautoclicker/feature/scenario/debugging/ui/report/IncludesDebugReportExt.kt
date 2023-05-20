/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.report

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.IncludeDebugReportMinAvgMaxBinding
import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.IncludeDebugReportTriggeredProcessedBinding
import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.IncludeDebugReportValueBinding

fun IncludeDebugReportValueBinding.setValue(@StringRes desc: Int, v: String) {
    description.setText(desc)
    value.text = v
}

fun IncludeDebugReportTriggeredProcessedBinding.setValues(
    @StringRes leftDesc: Int,
    leftVal: String,
    @StringRes rightDesc: Int,
    rightVal: String,
) {
    triggeredTitle.setText(leftDesc)
    triggeredCount.text = leftVal
    processedTitle.setText(rightDesc)
    processedCount.text = rightVal
}

fun IncludeDebugReportMinAvgMaxBinding.setValues(@StringRes descId: Int, min: String, avg: String, max: String) {
    description.setText(descId)
    minValue.text = min
    avgValue.text = avg
    maxValue.text = max
}