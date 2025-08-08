
package com.buzbuz.smartautoclicker.core.common.permissions.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle

import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.permissions.EXTRA_RESULT_KEY_PERMISSION_STATE
import com.buzbuz.smartautoclicker.core.common.permissions.FRAGMENT_RESULT_KEY_PERMISSION_STATE
import com.buzbuz.smartautoclicker.core.common.permissions.databinding.DialogPermissionBinding

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class PermissionDialogFragment : DialogFragment() {

    /** ViewModel providing the click scenarios data to the UI. */
    private val viewModel: PermissionDialogViewModel by viewModels()
    /** ViewBinding for this dialog. */
    private lateinit var viewBinding: DialogPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initResultLauncherIfNeeded(this) { isGranted, isOptional ->
            if (isGranted || isOptional) dismiss()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.dialogUiState.collect(::updateDialogUiState) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogPermissionBinding.inflate(layoutInflater).apply {
            buttonRequestPermission.setOnClickListener {
                viewModel.startPermissionFlow(requireActivity())
            }

            buttonDenyPermission.setOnClickListener {
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(viewBinding.root)
            .create()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.shouldBeDismissedOnResume(requireContext())) {
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(
            requestKey = FRAGMENT_RESULT_KEY_PERMISSION_STATE,
            result = bundleOf(EXTRA_RESULT_KEY_PERMISSION_STATE to viewModel.isPermissionGranted(requireContext())),
        )
    }

    private fun updateDialogUiState(state: PermissionDialogUiState?) {
        state ?: return

        viewBinding.apply {
            titlePermission.setText(state.titleRes)
            descPermission.setText(state.descriptionRes)
        }
    }
}