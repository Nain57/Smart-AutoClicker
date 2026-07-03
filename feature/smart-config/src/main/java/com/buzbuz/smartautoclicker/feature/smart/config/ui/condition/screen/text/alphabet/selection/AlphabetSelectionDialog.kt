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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.selection

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
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


class AlphabetSelectionDialog : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.ALPHABET_SELECTION.name

    private val viewModel: AlphabetSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { alphabetSelectionViewModel() },
    )

    private val alphabetAdapter: AlphabetModelItemAdapter = AlphabetModelItemAdapter(::onItemClicked)

    private lateinit var viewBinding: DialogBaseListBinding


    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_condition_selection)
                buttonSave.visibility = View.GONE
                buttonDelete.visibility = View.GONE
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
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingCondition.collect(::onConditionEditingStateChanged) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.items.collect(::onItemsUpdated) }
            }
        }
    }

    private fun onItemClicked(item: AlphabetSelectionItem) {
        if (item !is AlphabetSelectionItem.Alphabet) return

        when (item.downloadState){
            AlphabetDownloadUiState.Downloaded -> viewModel.selectModel(item.alphabet)
            AlphabetDownloadUiState.NotDownloaded -> viewModel.downloadModel(item.alphabet)
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

    private fun onConditionEditingStateChanged(isEditing: Boolean) {
        if (!isEditing) {
            Log.e(TAG, "Closing AlphabetSelectionDialog because there is no condition edited")
            finish()
        }
    }
}

private const val TAG = "AlphabetSelectionDialog"
