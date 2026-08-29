/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.ext.getConditionBitmap
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ConditionPerformanceViewModel @Inject constructor(
    debuggingRepository: DebuggingRepository,
    smartRepository: IRepository,
    private val bitmapRepository: BitmapRepository,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @param:Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val sort = MutableStateFlow(ConditionPerformanceSort.TOTAL_TIME)

    val uiState: StateFlow<ConditionPerformanceUiState> = debuggingRepository.getLastReportOverview()
        .flatMapLatest { overview ->
            if (overview == null) return@flatMapLatest flowOf(ConditionPerformanceUiState.NotAvailable)

            combine(
                smartRepository.getScreenEventsFlow(overview.scenarioId),
                smartRepository.getTriggerEventsFlow(overview.scenarioId),
                debuggingRepository.getLastReportConditionProfiles(),
                sort,
            ) { screenEvents, triggerEvents, profiles, selectedSort ->
                val screenConditions = screenEvents.toScreenConditionPerformanceSources()
                val triggerConditions = triggerEvents.toTriggerConditionPerformanceSources(screenConditions.size)
                val entries = buildConditionPerformanceReportOrNull(
                    conditions = screenConditions + triggerConditions,
                    profiles = profiles,
                    sort = selectedSort,
                ) ?: return@combine ConditionPerformanceUiState.NotAvailable
                ConditionPerformanceUiState.Available(
                    entries = entries,
                    sort = selectedSort,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConditionPerformanceUiState.Loading,
        )

    fun setSort(selectedSort: ConditionPerformanceSort) {
        sort.update { selectedSort }
    }

    fun getSort(): ConditionPerformanceSort = sort.value

    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        viewModelScope.launch(ioDispatcher) {
            try {
                val bitmap = bitmapRepository.getConditionBitmap(condition)
                withContext(mainDispatcher) { onBitmapLoaded(bitmap) }
            } catch (_: CancellationException) {
                withContext(mainDispatcher) { onBitmapLoaded(null) }
            }
        }
}
