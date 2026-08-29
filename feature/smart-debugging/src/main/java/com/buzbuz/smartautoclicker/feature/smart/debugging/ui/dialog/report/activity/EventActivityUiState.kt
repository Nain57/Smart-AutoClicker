/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity

sealed interface EventActivityUiState {
    data object Loading : EventActivityUiState
    data object NotAvailable : EventActivityUiState
    data object Empty : EventActivityUiState
    data class Available(
        val items: List<EventActivityListItem>,
        val sort: EventActivitySort,
    ) : EventActivityUiState
}

sealed interface EventActivityListItem {
    data class Header(val type: EventActivityType) : EventActivityListItem
    data class Event(val activity: EventActivityEntry) : EventActivityListItem
}
