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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.required

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetDownloadUiState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetModelItemAdapter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetSelectionItem

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType


class RequiredAlphabetDialog(
    private val onModelsReady: () -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.REQUIRED_ALPHABET.name

    private val viewModel: RequiredAlphabetViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { requiredAlphabetViewModel() },
    )

    private val alphabetAdapter: AlphabetModelItemAdapter = AlphabetModelItemAdapter(::onItemClicked)

    private lateinit var viewBinding: DialogBaseListBinding


    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_condition_selection)
                buttonSave.visibility = View.VISIBLE
                buttonDelete.visibility = View.GONE

                buttonSave.setDebouncedOnClickListener {
                    back()
                    onModelsReady()
                }
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            layoutLoadableList.apply {
                list.adapter = alphabetAdapter
                list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canContinue.collect(::onContinueButtonStateUpdated) }
                launch { viewModel.items.collect(::onItemsUpdated) }
            }
        }
    }

    private fun onItemClicked(item: AlphabetSelectionItem) {
        if (item !is AlphabetSelectionItem.Alphabet) return

        when (item.downloadState) {
            AlphabetDownloadUiState.NotDownloaded -> viewModel.downloadModel(item.alphabet)

            AlphabetDownloadUiState.Downloaded,
            is AlphabetDownloadUiState.Downloading,
            AlphabetDownloadUiState.Error -> Unit
        }
    }

    private fun onItemsUpdated(items: List<AlphabetSelectionItem>) {
        viewBinding.apply {
            viewBinding.layoutLoadableList.updateState(items)
            alphabetAdapter.submitList(items)
        }
    }

    private fun onContinueButtonStateUpdated(isEnabled: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isEnabled)
    }
}
