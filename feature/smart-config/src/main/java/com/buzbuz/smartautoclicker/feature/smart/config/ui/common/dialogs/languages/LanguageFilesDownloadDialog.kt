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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.languages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogLanguageFilesDownloadBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showStopDownloadWarningDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch


class LanguageFilesDownloadDialog(
    private val scenarioDbId: Long,
    private val onDownloadsCompleted: () -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private lateinit var viewBinding: DialogLanguageFilesDownloadBinding
    private val viewModel: LanguageFilesDownloadViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { languageFilesDownloadViewModel() },
    )

    private val languagesAdapter: LanguageFilesDownloadAdapter = LanguageFilesDownloadAdapter(
        onDownloadClicked = { viewModel.downloadLanguageFile(it) },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setScenarioId(scenarioDbId)

        viewBinding = DialogLanguageFilesDownloadBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_language_download)
                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                setButtonVisibility(DialogNavigationButton.DISMISS, View.VISIBLE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)

                buttonDismiss.setDebouncedOnClickListener {
                    back()
                }
                buttonSave.setDebouncedOnClickListener {
                    onDownloadsCompleted()
                    super.back()
                }
            }

            languagesList.apply {
                adapter = languagesAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.areAllLanguagesDownloaded.collect(::updateSaveButtonState) }
                launch { viewModel.uiItems.collect(languagesAdapter::submitList) }
            }
        }
    }

    override fun back() {
        if (viewModel.areItemsDownloading.value) {
            context.showStopDownloadWarningDialog {
                viewModel.cancelDownload()
                super.back()
            }
            return
        }

        super.back()
    }

    private fun updateSaveButtonState(isEnabled: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isEnabled)
    }
}