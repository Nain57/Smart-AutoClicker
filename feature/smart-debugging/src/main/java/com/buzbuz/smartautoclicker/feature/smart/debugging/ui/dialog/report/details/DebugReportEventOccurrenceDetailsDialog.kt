/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialog
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.condition.DebugConditionContent
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.counter.DebugCounterStateContent

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.launch
import kotlin.getValue


/** Displays the content of the current debug report. */
class DebugReportEventOccurrenceDetailsDialog(
    private val scenarioId: Long,
    private val eventOccurrence: DebugReportEventOccurrence,
): NavBarDialog(R.style.AppTheme) {

    /** View model for this dialog. */
    private val viewModel: DebugReportEventOccurrenceDetailsViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportEventOccurrenceViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        return super.onCreateView().also {
            topBarBinding.apply {
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
            }

            viewModel.setOccurrence(scenarioId, eventOccurrence)
        }
    }

    override fun inflateMenu(navBarView: NavigationBarView) {
        navBarView.inflateMenu(R.menu.menu_debug_event_occurence)
    }

    override fun onCreateContent(navItemId: Int): NavBarDialogContent =
        when (navItemId) {
            R.id.page_conditions -> DebugConditionContent(
                appContext = context.applicationContext,
                scenarioId = scenarioId,
                eventOccurrence = eventOccurrence,
            )

            R.id.page_counters -> DebugCounterStateContent(
                appContext = context.applicationContext,
                scenarioId = scenarioId,
                eventOccurrence = eventOccurrence,
            )

            else -> throw IllegalArgumentException("Unknown menu id $navItemId")
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        super.onDialogCreated(dialog)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) {
        when (buttonType) {
            DialogNavigationButton.DISMISS -> {
                back()
                return
            }

            DialogNavigationButton.SAVE -> Unit
            DialogNavigationButton.DELETE -> Unit
        }
    }

    private fun updateUiState(uiState: DebugReportEventOccurrenceUiState?) {
        uiState ?: return
        topBarBinding.dialogTitle.text = uiState.dialogTitle
    }
}