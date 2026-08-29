/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions

sealed interface ConditionPerformanceUiState {
    data object Loading : ConditionPerformanceUiState
    data object NotAvailable : ConditionPerformanceUiState
    data class Available(
        val entries: List<ConditionPerformanceEntry>,
        val sort: ConditionPerformanceSort,
    ) : ConditionPerformanceUiState
}
