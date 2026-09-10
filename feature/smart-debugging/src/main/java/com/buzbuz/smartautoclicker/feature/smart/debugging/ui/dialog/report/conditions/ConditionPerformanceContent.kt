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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ContentConditionPerformanceBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions.adapter.ConditionPerformanceAdapter
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.sort.DebugReportSortOption
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.sort.DebugReportSortPopup
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.captureScrollPosition
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.restoreScrollPosition
import kotlinx.coroutines.launch

class ConditionPerformanceContent(appContext: Context) : NavBarDialogContent(appContext) {

    private val viewModel: ConditionPerformanceViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { conditionPerformanceViewModel() },
    )
    private val adapter = ConditionPerformanceAdapter(bitmapProvider = { condition, callback ->
        viewModel.getConditionBitmap(condition, callback)
    })
    private var sortPopup: DebugReportSortPopup<ConditionPerformanceSort>? = null
    private lateinit var binding: ContentConditionPerformanceBinding

    override fun floatingActionButtonsAreAvailable(): Boolean = true
    override fun primaryFloatingActionButtonIcon(): Int = R.drawable.ic_sort

    override fun onCreateView(container: ViewGroup): ViewGroup {
        binding = ContentConditionPerformanceBinding.inflate(LayoutInflater.from(context), container, false).apply {
            list.adapter = adapter
            fastScroller.attachToRecyclerView(list)
        }
        return binding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }
    }

    override fun onStart() {
        dialogController.floatingActionButtons.primary.contentDescription =
            context.getString(R.string.content_desc_condition_performance_sort)
    }

    override fun onStop() {
        sortPopup?.dismiss()
        sortPopup = null
    }

    private fun updateUiState(uiState: ConditionPerformanceUiState) {
        when (uiState) {
            ConditionPerformanceUiState.Loading -> showLoading()
            ConditionPerformanceUiState.NotAvailable -> showNotAvailable()
            is ConditionPerformanceUiState.Available -> showAvailable(uiState)
        }
    }

    private fun showLoading() = binding.apply {
        loading.visibility = View.VISIBLE
        unavailable.visibility = View.GONE
        list.visibility = View.GONE
        fastScroller.visibility = View.GONE
        dialogController.floatingActionButtons.root.visibility = View.GONE
    }

    private fun showNotAvailable() = binding.apply {
        loading.visibility = View.GONE
        unavailable.visibility = View.VISIBLE
        list.visibility = View.GONE
        fastScroller.visibility = View.GONE
        adapter.submitList(emptyList())
        dialogController.floatingActionButtons.root.visibility = View.GONE
    }

    private fun showAvailable(uiState: ConditionPerformanceUiState.Available) = binding.apply {
        loading.visibility = View.GONE
        unavailable.visibility = View.GONE
        list.visibility = View.VISIBLE
        fastScroller.visibility = View.VISIBLE
        dialogController.floatingActionButtons.root.visibility = View.VISIBLE
        val previousScrollPosition = list.captureScrollPosition()
        adapter.submitEntries(uiState.entries) {
            list.restoreScrollPosition(previousScrollPosition)
            list.postOnAnimation {
                list.restoreScrollPosition(previousScrollPosition)
                fastScroller.refresh()
            }
        }
    }

    override fun onPrimaryFloatingActionButtonClicked() {
        val selectedSort = viewModel.getSort()
        sortPopup?.dismiss()
        sortPopup = DebugReportSortPopup(
            anchor = dialogController.floatingActionButtons.primary,
            options = ConditionPerformanceSort.entries.map { sort ->
                DebugReportSortOption(
                    value = sort,
                    titleRes = when (sort) {
                        ConditionPerformanceSort.TOTAL_TIME -> R.string.condition_performance_sort_total_time
                        ConditionPerformanceSort.AVERAGE_PER_CHECK -> R.string.condition_performance_sort_average
                        ConditionPerformanceSort.CHECKS -> R.string.condition_performance_sort_checks
                        ConditionPerformanceSort.SCENARIO_ORDER -> R.string.condition_performance_sort_scenario_order
                    },
                    selected = sort == selectedSort,
                )
            },
            onSelected = viewModel::setSort,
        ).also { it.show() }
    }
}
