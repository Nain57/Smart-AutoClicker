/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.flags

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonVisibility

import com.buzbuz.smartautoclicker.core.ui.bindings.setOnDismissClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setup
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.BackToPreviousOverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogConfigActionIntentFlagsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class FlagsSelectionDialog (
    private val currentFlags: Int,
    private val startActivityFlags: Boolean,
    private val onConfigComplete: (flags: Int) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: FlagsSelectionViewModel by viewModels()

    private lateinit var viewBinding: DialogConfigActionIntentFlagsBinding
    private lateinit var flagsAdapter: FlagsSelectionAdapter

    override fun onCreateView(): ViewGroup {
        flagsAdapter = FlagsSelectionAdapter(
            onFlagCheckClicked = viewModel::setFlagState,
            onFlagHelpClicked = ::onFlagHelpClicked,
        )

        viewBinding = DialogConfigActionIntentFlagsBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_intent_flags)

                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        onConfigComplete(viewModel.getSelectedFlags())
                        back()
                    }
                }
            }

            flagsList.apply {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                adapter = flagsAdapter
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setSelectedFlags(currentFlags, startActivityFlags)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.flagsItems.collect(flagsAdapter::submitList) }
            }
        }
    }

    private fun onFlagHelpClicked(uri: Uri) {
        debounceUserInteraction {
            try {
                context.startActivity(
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
                        data = uri
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )

                OverlayManager.getInstance(context).navigateTo(
                    context = context,
                    newOverlay = BackToPreviousOverlayMenu(),
                    hideCurrent = true,
                )
            } catch (ex: ActivityNotFoundException) {
                Log.e(LOG_TAG, "Can't open browser to show documentation.")
            }
        }
    }
}

private const val LOG_TAG = "FlagsSelectionDialog"