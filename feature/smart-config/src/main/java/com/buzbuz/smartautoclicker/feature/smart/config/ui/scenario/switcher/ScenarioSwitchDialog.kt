/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.switcher

import android.content.res.Configuration
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.model.ScenarioSwitchResult
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogScenarioSwitchBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ScenarioSwitchDialog(
    private val onScenarioSelected: suspend (Scenario) -> ScenarioSwitchResult,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ScenarioSwitchViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { scenarioSwitchViewModel() },
    )

    private lateinit var binding: DialogScenarioSwitchBinding
    private lateinit var adapter: ScenarioSwitchAdapter
    private var isSwitching = false
    private var failedScenario: Scenario? = null

    override fun onCreateView(): ViewGroup {
        binding = DialogScenarioSwitchBinding.inflate(LayoutInflater.from(context)).apply {
            toolbar.setNavigationOnClickListener { debounceUserInteraction { if (!isSwitching) back() } }
        }

        adapter = ScenarioSwitchAdapter(::onScenarioClicked)
        binding.list.adapter = adapter
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.list.layoutManager = GridLayoutManager(context, 2)
        }
        return binding.root
    }

    override fun onDialogCreated(dialog: com.google.android.material.bottomsheet.BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUi)
            }
        }
    }

    override fun back() {
        if (!isSwitching) super.back()
    }

    override fun onStop() {
        // OverlayDialog recreates this object during rotation. A cancelled selection must not leave the recreated
        // picker permanently disabled or showing a spinner for a job that no longer exists.
        isSwitching = false
        failedScenario = null
        super.onStop()
    }

    private fun updateUi(state: ScenarioSwitchUiState) {
        binding.toolbar.setAutoSizedSubtitle(
            context.getString(
                R.string.scenario_switcher_current,
                state.currentScenario?.name ?: context.getString(R.string.scenario_switcher_current_unknown),
            ),
        )

        adapter.submitList(state.alternatives)
        adapter.setSwitchingScenario(if (isSwitching) failedScenario?.id else null)
        adapter.setSelectionEnabled(!state.isLoading && !isSwitching && state.isPaused && state.currentScenario != null)
        binding.progressLoading.isVisible = state.isLoading
        binding.emptyMessage.isVisible = !state.isLoading && state.alternatives.isEmpty()
        binding.list.isVisible = !state.isLoading && state.alternatives.isNotEmpty()
        binding.list.isEnabled = !isSwitching && state.isPaused
    }

    private fun onScenarioClicked(scenario: Scenario) {
        if (isSwitching) return
        val state = viewModel.uiState.value
        if (!state.isPaused || state.currentScenario == null) {
            showError(R.string.scenario_switcher_error_paused)
            return
        }
        if (state.alternatives.none { it.id == scenario.id }) {
            showError(R.string.scenario_switcher_error_unavailable)
            return
        }
        startSwitch(scenario)
    }

    private fun startSwitch(scenario: Scenario) {
        isSwitching = true
        failedScenario = scenario
        updateUi(viewModel.uiState.value)

        lifecycleScope.launch {
            try {
                when (onScenarioSelected(scenario)) {
                    ScenarioSwitchResult.Success -> dismissAfterSuccessfulSwitch()
                    ScenarioSwitchResult.ServiceUnavailable -> showError(R.string.scenario_switcher_error_service)
                    ScenarioSwitchResult.InvalidProcessingState -> showError(R.string.scenario_switcher_error_paused)
                    ScenarioSwitchResult.ProjectionUnavailable -> showError(R.string.scenario_switcher_error_projection)
                    ScenarioSwitchResult.CurrentScenario -> showError(R.string.scenario_switcher_error_current)
                    ScenarioSwitchResult.ScenarioUnavailable -> showError(R.string.scenario_switcher_error_unavailable)
                    ScenarioSwitchResult.PersistenceFailure -> showError(R.string.scenario_switcher_error_persistence)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(R.string.scenario_switcher_error_unknown)
            }
        }
    }

    private fun showError(messageRes: Int) {
        isSwitching = false
        updateUi(viewModel.uiState.value)

        Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).apply {
            failedScenario
                ?.let { failed -> viewModel.uiState.value.alternatives.firstOrNull { it.id == failed.id } }
                ?.let { availableScenario ->
                    setAction(R.string.scenario_switcher_retry) { startSwitch(availableScenario) }
                }
        }.show()
    }

    private fun dismissAfterSuccessfulSwitch() {
        super.back()
    }

    private fun MaterialToolbar.setAutoSizedSubtitle(text: CharSequence) {
        subtitle = text
        children
            .filterIsInstance<TextView>()
            .firstOrNull { it.text == text }
            ?.apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this,
                    SUBTITLE_MIN_TEXT_SIZE_SP,
                    SUBTITLE_MAX_TEXT_SIZE_SP,
                    SUBTITLE_TEXT_SIZE_STEP_SP,
                    TypedValue.COMPLEX_UNIT_SP,
                )
            }
    }

    private companion object {
        const val SUBTITLE_MIN_TEXT_SIZE_SP = 12
        const val SUBTITLE_MAX_TEXT_SIZE_SP = 14
        const val SUBTITLE_TEXT_SIZE_STEP_SP = 1
    }
}
