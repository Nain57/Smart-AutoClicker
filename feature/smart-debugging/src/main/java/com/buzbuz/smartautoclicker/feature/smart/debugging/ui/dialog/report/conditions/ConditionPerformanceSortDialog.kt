/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.DialogChoice
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MultiChoiceDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.R

class ConditionPerformanceSortDialog(
    selectedSort: ConditionPerformanceSort,
    onSortSelected: (ConditionPerformanceSort) -> Unit,
) : MultiChoiceDialog<ConditionPerformanceSortChoice>(
    theme = R.style.AppTheme,
    dialogTitleText = R.string.dialog_overlay_title_condition_performance_sort,
    choices = ConditionPerformanceSort.entries.map { sort ->
        ConditionPerformanceSortChoice(sort, sort == selectedSort)
    },
    onChoiceSelected = { choice -> onSortSelected(choice.sort) },
)

class ConditionPerformanceSortChoice(
    val sort: ConditionPerformanceSort,
    selected: Boolean,
) : DialogChoice(
    title = when (sort) {
        ConditionPerformanceSort.TOTAL_TIME -> R.string.condition_performance_sort_total_time
        ConditionPerformanceSort.AVERAGE_PER_CHECK -> R.string.condition_performance_sort_average
        ConditionPerformanceSort.CHECKS -> R.string.condition_performance_sort_checks
        ConditionPerformanceSort.SCENARIO_ORDER -> R.string.condition_performance_sort_scenario_order
    },
    description = when (sort) {
        ConditionPerformanceSort.TOTAL_TIME -> R.string.condition_performance_sort_total_time_desc
        ConditionPerformanceSort.AVERAGE_PER_CHECK -> R.string.condition_performance_sort_average_desc
        ConditionPerformanceSort.CHECKS -> R.string.condition_performance_sort_checks_desc
        ConditionPerformanceSort.SCENARIO_ORDER -> R.string.condition_performance_sort_scenario_order_desc
    },
    iconId = if (selected) R.drawable.ic_debug_confirm else null,
)
