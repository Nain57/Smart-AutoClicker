
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.component

import android.content.ComponentName
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.core.android.application.AndroidApplicationInfo
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [OverlayDialog] implementation for displaying a list of Android activities.
 *
 * @param onApplicationSelected called when the user clicks on an application.
 */
class ComponentSelectionDialog(
    private val onApplicationSelected: (ComponentName) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ComponentSelectionModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { componentSelectionViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    /** Handle the binding between the application list and the views displaying them. */
    private lateinit var activitiesAdapter: ComponentSelectionAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_intent_component_name)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            activitiesAdapter = ComponentSelectionAdapter { selectedComponentName ->
                debounceUserInteraction {
                    onApplicationSelected(selectedComponentName)
                    back()
                }
            }

            layoutLoadableList.list.apply {
                adapter = activitiesAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activities.collect(::updateActivityList)
            }
        }
    }

    private fun updateActivityList(activities: List<AndroidApplicationInfo>) {
        viewBinding.layoutLoadableList.updateState(activities)
        activitiesAdapter.submitList(activities)
    }
}