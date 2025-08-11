
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.IncludeDebugReportMinAvgMaxBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.IncludeDebugReportTriggeredProcessedBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.IncludeDebugReportValueBinding

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