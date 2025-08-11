
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.intent

import android.net.Uri
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
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionIntentActionsBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters.newWebBrowserStarterOverlay

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
            onActionHelpClicked = { uri -> debounceUserInteraction { onActionHelpClicked(uri) } },
        )

        viewBinding = DialogConfigActionIntentActionsBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_intent_actions)

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
        overlayManager.navigateTo(
            context = context,
            newOverlay = newWebBrowserStarterOverlay(uri),
            hideCurrent = true,
        )
    }
}
