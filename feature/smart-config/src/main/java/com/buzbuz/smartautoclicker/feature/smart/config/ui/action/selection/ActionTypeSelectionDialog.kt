
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection

import android.view.View

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MultiChoiceDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

class ActionTypeSelectionDialog(
    choices: List<ActionTypeChoice>,
    onChoiceSelectedListener: (ActionTypeChoice) -> Unit,
    onCancelledListener: (() -> Unit)? = null,
) : MultiChoiceDialog<ActionTypeChoice>(
    theme = R.style.ScenarioConfigTheme,
    dialogTitleText = R.string.dialog_title_action_type,
    choices = choices,
    onChoiceSelected = onChoiceSelectedListener,
    onCanceled = onCancelledListener,
) {

    /** View model for this content. */
    private val viewModel: ActionTypeSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { actionTypeSelectionViewModel() },
    )

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    override fun onChoiceViewBound(choice: ActionTypeChoice, view: View?) {
        if (choice !is ActionTypeChoice.Click) return

        if (view != null) viewModel.monitorCreateClickView(view)
        else viewModel.stopViewMonitoring()
    }
}