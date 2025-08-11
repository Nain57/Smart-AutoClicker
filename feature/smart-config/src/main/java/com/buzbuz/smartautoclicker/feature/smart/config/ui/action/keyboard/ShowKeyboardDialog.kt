package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionShowKeyboardBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import kotlinx.coroutines.launch

class ShowKeyboardDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ShowKeyboardViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { showKeyboardViewModel() },
    )

    private lateinit var binding: DialogConfigActionShowKeyboardBinding

    override fun onCreateView(): ViewGroup {
        binding = DialogConfigActionShowKeyboardBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_show_keyboard)
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
        binding.fieldPositionType.setOnItemSelectedListener { idx -> viewModel.setPositionTypeByIndex(idx) }
        binding.fieldTargetCondition.setOnItemSelectedListener { idx -> viewModel.setTargetConditionByIndex(idx) }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.name.collect { binding.fieldName.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.positionTypeIndex.collect { binding.fieldPositionType.setSelectionSafe(it) } }
                launch { viewModel.conditionNames.collect {
                    binding.fieldTargetCondition.setItems(it)
                    binding.fieldTargetCondition.setSelectionSafe(viewModel.currentConditionIndex())
                } }
                launch { viewModel.canSave.collect { binding.layoutTopBar.buttonSave.isEnabled = it } }
            }
        }
    }

    private fun onSave() { viewModel.saveLastConfig(); listener.onConfirmClicked(); back() }
    private fun onDelete() { viewModel.deleteAction(); listener.onDeleteClicked(); back() }
}