package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionHideKeyboardBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import kotlinx.coroutines.launch

class HideKeyboardDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: HideKeyboardViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { hideKeyboardViewModel() },
    )

    private lateinit var binding: DialogConfigActionHideKeyboardBinding

    override fun onCreateView(): ViewGroup {
        binding = DialogConfigActionHideKeyboardBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_hide_keyboard)
                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.apply { visibility = View.VISIBLE; setDebouncedOnClickListener { onSave() } }
                buttonDelete.apply { visibility = View.VISIBLE; setDebouncedOnClickListener { onDelete() } }
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.fieldName.setOnTextChanged { viewModel.setName(it) }
        binding.fieldMethod.setOnItemSelectedListener { idx -> viewModel.setMethodByIndex(idx) }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.name.collect { binding.fieldName.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.methodIndex.collect { binding.fieldMethod.setSelectionSafe(it) } }
                launch { viewModel.canSave.collect { binding.layoutTopBar.buttonSave.isEnabled = it } }
            }
        }
    }

    private fun onSave() { viewModel.saveLastConfig(); listener.onConfirmClicked(); back() }
    private fun onDelete() { viewModel.deleteAction(); listener.onDeleteClicked(); back() }
}