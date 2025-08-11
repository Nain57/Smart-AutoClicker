package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.longpress

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionLongPressBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import kotlinx.coroutines.launch

class LongPressDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: LongPressViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { longPressViewModel() },
    )

    private lateinit var binding: DialogConfigActionLongPressBinding

    override fun onCreateView(): ViewGroup {
        binding = DialogConfigActionLongPressBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_long_press)
                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSave() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onDelete() }
                }
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // Wire inputs -> VM
        binding.fieldName.setOnTextChanged { viewModel.setName(it) }
        binding.fieldHoldMs.setOnTextChanged { ms ->
            ms.toLongOrNull()?.let(viewModel::setHoldDuration)
        }
        binding.fieldPositionType.setOnItemSelectedListener { idx ->
            viewModel.setPositionTypeByIndex(idx) // 0: ON_DETECTED_CONDITION, 1: USER_SELECTED
        }
        binding.fieldTargetCondition.setOnItemSelectedListener { idx ->
            viewModel.setTargetConditionByIndex(idx)
        }

        // Collect UI -> fields
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.name.collect { binding.fieldName.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.holdDuration.collect { binding.fieldHoldMs.setTextIfDifferent(it ?: 600L) } }
                launch { viewModel.positionTypeIndex.collect { binding.fieldPositionType.setSelectionSafe(it) } }
                launch { viewModel.conditionNames.collect {
                    binding.fieldTargetCondition.setItems(it)
                    binding.fieldTargetCondition.setSelectionSafe(viewModel.currentConditionIndex())
                } }
                launch { viewModel.canSave.collect { binding.layoutTopBar.buttonSave.isEnabled = it } }
            }
        }
    }

    private fun onSave() {
        viewModel.saveLastConfig()
        listener.onConfirmClicked()
        back()
    }

    private fun onDelete() {
        viewModel.deleteAction()
        listener.onDeleteClicked()
        back()
    }

    override fun back() {
        // Optional: confirm if unsaved like ClickDialog does
        listener.onDismissClicked()
        super.back()
    }
}