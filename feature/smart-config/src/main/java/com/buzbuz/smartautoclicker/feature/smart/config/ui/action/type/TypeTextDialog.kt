package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.type

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionTypeTextBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import kotlinx.coroutines.launch

class TypeTextDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: TypeTextViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { typeTextViewModel() },
    )

    private lateinit var binding: DialogConfigActionTypeTextBinding

    override fun onCreateView(): ViewGroup {
        binding = DialogConfigActionTypeTextBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_type_text)
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
        binding.fieldText.setOnTextChanged { viewModel.setText(it) }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.name.collect { binding.fieldName.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.text.collect { binding.fieldText.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.canSave.collect { binding.layoutTopBar.buttonSave.isEnabled = it } }
            }
        }
    }

    private fun onSave() { viewModel.saveLastConfig(); listener.onConfirmClicked(); back() }
    private fun onDelete() { viewModel.deleteAction(); listener.onDeleteClicked(); back() }
}