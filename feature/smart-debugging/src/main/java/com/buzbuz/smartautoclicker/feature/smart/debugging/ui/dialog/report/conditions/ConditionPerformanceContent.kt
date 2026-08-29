/* Copyright (C) 2026 Kevin Buzeau */
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
import kotlinx.coroutines.launch

class ConditionPerformanceContent(appContext: Context) : NavBarDialogContent(appContext) {

    private val viewModel: ConditionPerformanceViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { conditionPerformanceViewModel() },
    )
    private val adapter = ConditionPerformanceAdapter(bitmapProvider = { condition, callback ->
        viewModel.getConditionBitmap(condition, callback)
    })
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
        adapter.submitEntries(uiState.entries) { fastScroller.refresh() }
    }

    override fun onPrimaryFloatingActionButtonClicked() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = ConditionPerformanceSortDialog(
                selectedSort = viewModel.getSort(),
                onSortSelected = viewModel::setSort,
            ),
            hideCurrent = false,
        )
    }
}
