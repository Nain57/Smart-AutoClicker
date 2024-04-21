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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.intent

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
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.BackToPreviousOverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionIntentActionsBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class IntentActionsSelectionDialog (
    private val currentAction: String?,
    private val onConfigComplete: (action: String?) -> Unit,
    private val forBroadcastReception: Boolean = false,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: IntentActionsSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { intentActionsSelectionViewModel() },
    )

    private lateinit var viewBinding: DialogConfigActionIntentActionsBinding
    private lateinit var actionsAdapter: IntentActionsSelectionAdapter

    override fun onCreateView(): ViewGroup {
        viewModel.setRequestedActionsType(forBroadcastReception)

        actionsAdapter = IntentActionsSelectionAdapter(
            onActionCheckClicked = viewModel::setActionSelectionState,
            onActionHelpClicked = ::onActionHelpClicked,
        )

        viewBinding = DialogConfigActionIntentActionsBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_intent_actions)

                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        onConfigComplete(viewModel.getSelectedAction())
                        back()
                    }
                }
            }

            actionsList.apply {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                adapter = actionsAdapter
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setSelectedAction(currentAction)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.actionsItems.collect(actionsAdapter::submitList) }
            }
        }
    }

    private fun onActionHelpClicked(uri: Uri) {
        debounceUserInteraction {
            try {
                context.startActivity(
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
                        .apply {
                        data = uri
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )

                overlayManager.navigateTo(
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

private const val LOG_TAG = "ActionsSelectionDialog"