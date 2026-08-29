/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.DialogChoice
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MultiChoiceDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.R


class EventActivitySortDialog(
    selectedSort: EventActivitySort,
    onSortSelected: (EventActivitySort) -> Unit,
) : MultiChoiceDialog<EventActivitySortChoice>(
    theme = R.style.AppTheme,
    dialogTitleText = R.string.dialog_overlay_title_event_activity_sort,
    choices = EventActivitySort.entries.map { sort -> EventActivitySortChoice(sort, sort == selectedSort) },
    onChoiceSelected = { choice -> onSortSelected(choice.sort) },
)

class EventActivitySortChoice(
    val sort: EventActivitySort,
    selected: Boolean,
) : DialogChoice(
    title = when (sort) {
        EventActivitySort.SCENARIO_ORDER -> R.string.event_activity_sort_scenario_order
        EventActivitySort.MOST_FREQUENT -> R.string.event_activity_sort_most_frequent
        EventActivitySort.FIRST_EXECUTION -> R.string.event_activity_sort_first_execution
    },
    description = when (sort) {
        EventActivitySort.SCENARIO_ORDER -> R.string.event_activity_sort_scenario_order_desc
        EventActivitySort.MOST_FREQUENT -> R.string.event_activity_sort_most_frequent_desc
        EventActivitySort.FIRST_EXECUTION -> R.string.event_activity_sort_first_execution_desc
    },
    iconId = if (selected) R.drawable.ic_debug_confirm else null,
)
