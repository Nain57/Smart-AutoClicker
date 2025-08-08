
package com.buzbuz.smartautoclicker.scenarios.list.copy

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.WindowManager

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.databinding.DialogScenarioCopyBinding

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScenarioCopyDialog : DialogFragment() {

    companion object {

        /** Tag for copy fragment. */
        const val FRAGMENT_TAG_COPY_DIALOG = "ScenarioCopyDialog"
        /** Key for this fragment argument. Tells the database identifier of the scenario to copy (Long). */
        private const val FRAGMENT_ARG_KEY_SCENARIO_ID = ":copy:fragment_args_key_scenario_id"
        /** Key for this fragment argument. Tells if this is a smart scenario. (Boolean). */
        private const val FRAGMENT_ARG_KEY_IS_SMART = ":copy:fragment_args_key_is_smart"
        /** Key for this fragment argument. Contains the default name for the copy. (String). */
        private const val FRAGMENT_ARG_KEY_DEFAULT_COPY_NAME = ":copy:fragment_args_key_default_copy_name"

        /**
         * Creates a new instance of this fragment.
         * @param scenarioId database identifier for the scenario to copy.
         * @param isSmart tells if this is a smart scenario
         * @param defaultName name for the copy, can be null.
         * @return the new fragment.
         */
        fun newInstance(
            scenarioId: Long,
            isSmart: Boolean,
            defaultName: String? = null,
        ) = ScenarioCopyDialog().apply {
            arguments = Bundle().apply {
                putLong(FRAGMENT_ARG_KEY_SCENARIO_ID, scenarioId)
                putBoolean(FRAGMENT_ARG_KEY_IS_SMART, isSmart)
                defaultName?.let {
                    putString(FRAGMENT_ARG_KEY_DEFAULT_COPY_NAME, defaultName)
                }
            }
        }
    }

    /** The view model containing the backup state. */
    private val viewModel: ScenarioCopyViewModel by viewModels()
    /** The view binding on the views of this dialog.*/
    private lateinit var viewBinding: DialogScenarioCopyBinding

    private val scenarioId: Long by lazy { requireArguments().getLong(FRAGMENT_ARG_KEY_SCENARIO_ID) }
    private val isSmartScenario: Boolean by lazy { requireArguments().getBoolean(FRAGMENT_ARG_KEY_IS_SMART) }
    private val defaultName: String by lazy { requireArguments().getString(FRAGMENT_ARG_KEY_DEFAULT_COPY_NAME) ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.copyNameError.collect(viewBinding.fieldScenarioName::setError)
            }
        }
        viewModel.setCopyName(defaultName)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogScenarioCopyBinding.inflate(layoutInflater).apply {
            fieldScenarioName.apply {
                setLabel(R.string.default_click_name)
                setText(defaultName, InputType.TYPE_CLASS_TEXT)
                setOnTextChangedListener { viewModel.setCopyName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(requireContext().resources.getInteger(R.integer.name_max_length))
                )
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Copy Scenario")
            .setView(viewBinding.root)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        return dialog
    }

    override fun onStart() {
        super.onStart()

        viewBinding.fieldScenarioName.textField.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun onConfirm() {
        viewModel.copyScenario(scenarioId, isSmartScenario) {
            dismiss()
        }
    }
}