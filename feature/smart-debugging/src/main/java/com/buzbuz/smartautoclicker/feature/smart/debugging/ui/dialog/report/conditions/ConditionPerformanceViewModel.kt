/*
 * Copyright (C) 2026 Kevin Buzeau
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
                val entries = buildConditionPerformanceReport(
                    conditions = screenConditions + triggerConditions,
                    profiles = profiles,
                    sort = selectedSort,
                )
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
