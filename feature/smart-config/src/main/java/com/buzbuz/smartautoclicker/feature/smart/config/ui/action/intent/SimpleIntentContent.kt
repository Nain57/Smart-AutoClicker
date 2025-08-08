
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.android.application.AndroidApplicationInfo
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.dialogViewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setImageDrawable
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ContentIntentConfigSimpleBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.activities.ActivitySelectionDialog

import kotlinx.coroutines.launch

class SimpleIntentContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val dialogViewModel: IntentViewModel by dialogViewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { intentViewModel() },
    )

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentIntentConfigSimpleBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentIntentConfigSimpleBinding.inflate(LayoutInflater.from(context)).apply {
            fieldName.apply {
                setLabel(R.string.generic_name)
                setOnTextChangedListener { dialogViewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(fieldName.textField)

            fieldSelectionApplication.apply {
                setOnClickListener { debounceUserInteraction { showApplicationSelectionDialog() } }
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.name.collect(::updateClickName) }
                launch { dialogViewModel.nameError.collect(viewBinding.fieldName::setError)}
                launch { dialogViewModel.activityInfo.collect(::updateActivityInfo) }
            }
        }
    }

    private fun updateClickName(newName: String?) {
        viewBinding.fieldName.setText(newName)
    }

    private fun updateActivityInfo(activityInfo: AndroidApplicationInfo?) {
        viewBinding.fieldSelectionApplication.apply {
            if (activityInfo == null) {
                setTitle(context.getString(R.string.field_application_selection_title))
                setDescription(context.getString(R.string.field_application_selection_desc))
            } else {
                setImageDrawable(activityInfo.icon)
                setTitle(activityInfo.name)
                setDescription(activityInfo.componentName.packageName)
            }
        }
    }

    private fun showApplicationSelectionDialog() =
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = ActivitySelectionDialog(
                onApplicationSelected = { componentName ->
                    dialogViewModel.setActivitySelected(componentName)
                }
            ),
            hideCurrent = false,
        )
}