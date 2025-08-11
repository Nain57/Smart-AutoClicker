
package com.buzbuz.smartautoclicker.scenarios.creation

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDialogNavigationTopBarBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldTextInputBinding
import com.buzbuz.smartautoclicker.databinding.DialogScenarioCreationBinding
import com.buzbuz.smartautoclicker.databinding.IncludeScenarioTypeViewBinding

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScenarioCreationDialog : DialogFragment() {

    companion object {
        /** Tag for pro mode scenario creation dialog fragment. */
        const val FRAGMENT_TAG = "ScenarioCreationDialog"
    }

    private val viewModel: ScenarioCreationViewModel by viewModels()

    /** The view binding on the views of this dialog. */
    private lateinit var viewBinding: DialogScenarioCreationBinding
    /** The Android InputMethodManger, for managing the keyboard dismiss. */
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputMethodManager = requireContext().getSystemService(InputMethodManager::class.java)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::setInitialScenarioName) }
                launch { viewModel.nameError.collect(viewBinding.scenarioNameInputLayout::setError) }
                launch { viewModel.scenarioTypeSelectionState.collect(::updateTypeSelection) }
                launch { viewModel.creationState.collect(::updateCreationState) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogScenarioCreationBinding.inflate(layoutInflater).apply {
            layoutTopBar.initTopBar()
            scenarioNameInputLayout.initScenarioNameField()
            scenarioTypeSmart.initScenarioTypeCard(ScenarioTypeSelection.SMART)
        }

        return createDialog(viewBinding.root)
    }

    private fun createDialog(root: View): BottomSheetDialog =
        BottomSheetDialog(requireContext()).apply {
            setContentView(root)
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    this@ScenarioCreationDialog.dismiss()
                    true
                } else false
            }

            create()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    private fun IncludeDialogNavigationTopBarBinding.initTopBar() {
        dialogTitle.setText(R.string.dialog_title_add_scenario)

        buttonDismiss.apply {
            visibility = View.VISIBLE
            setOnClickListener { dismiss() }
        }
        buttonSave.apply {
            visibility = View.VISIBLE
            setOnClickListener { viewModel.createScenario() }
        }
        buttonDelete.visibility = View.GONE
    }

    private fun IncludeFieldTextInputBinding.initScenarioNameField() {
        setLabel(R.string.input_field_label_scenario_name)
        setOnTextChangedListener { viewModel.setName(it.toString()) }
        textField.filters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(requireContext().resources.getInteger(R.integer.name_max_length))
        )
    }

    private fun IncludeScenarioTypeViewBinding.initScenarioTypeCard(type: ScenarioTypeSelection) {
        when (type) {
            ScenarioTypeSelection.SMART -> {
                titleScenarioType.setText(R.string.item_title_smart_scenario)
                imageScenarioType.setImageResource(R.drawable.ic_smart)
            }
        }

        root.setOnClickListener { viewModel.setSelectedType(type) }
    }

    private fun setInitialScenarioName(name: String?) {
        viewBinding.scenarioNameInputLayout.setText(name)
    }

    private fun updateTypeSelection(state: ScenarioTypeSelectionState) {
        viewBinding.apply {
            scenarioTypeSmart.setState(state.smartItem, state.selectedItem, ScenarioTypeSelection.SMART)

            when (state.selectedItem) {
                ScenarioTypeSelection.SMART -> {
                    scenarioTypeDescription.setText(state.smartItem.descriptionText)
                    scenarioTypeDescriptionNotPurchased.visibility =  View.GONE
                }
            }
        }
    }

    private fun IncludeScenarioTypeViewBinding.setState(
        item: ScenarioTypeItem,
        selectedItem: ScenarioTypeSelection,
        type: ScenarioTypeSelection,
    ) {
        root.isChecked = selectedItem == type
        titleScenarioType.setText(item.titleRes)
        imageScenarioType.setImageResource(item.iconRes)

        root.setOnClickListener { viewModel.setSelectedType(type) }
    }

    private fun updateCreationState(state: CreationState) {
        viewBinding.layoutTopBar.apply {
            when (state) {
                CreationState.CONFIGURING_INVALID ->
                    setButtonEnabledState(DialogNavigationButton.SAVE, false)
                CreationState.CONFIGURING ->
                    setButtonEnabledState(DialogNavigationButton.SAVE, true)
                CreationState.CREATING ->
                    setButtonEnabledState(DialogNavigationButton.SAVE, false)
                CreationState.SAVED ->
                    dismiss()
            }
        }
    }
}