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

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetDownloadUiState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetModelItemAdapter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetSelectionItem

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RequiredAlphabetFragment : BottomSheetDialogFragment() {

    companion object {
        const val FRAGMENT_TAG = "RequiredAlphabetFragment"
    }

    private val viewModel: RequiredAlphabetViewModel by viewModels()
    private val alphabetAdapter: AlphabetModelItemAdapter = AlphabetModelItemAdapter(::onItemClicked)

    private lateinit var viewBinding: DialogBaseListBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = DialogBaseListBinding.inflate(inflater, container, false).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_condition_selection)
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { dismiss() }
                }
                buttonDelete.visibility = View.GONE
                buttonDismiss.setOnClickListener { dismiss() }
            }
            layoutLoadableList.apply {
                list.adapter = alphabetAdapter
                list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }
        }
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canContinue.collect(::onContinueButtonStateUpdated) }
                launch { viewModel.items.collect(::onItemsUpdated) }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
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
        viewBinding.layoutLoadableList.updateState(items)
        alphabetAdapter.submitList(items)
    }

    private fun onContinueButtonStateUpdated(isEnabled: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isEnabled)
    }
}
