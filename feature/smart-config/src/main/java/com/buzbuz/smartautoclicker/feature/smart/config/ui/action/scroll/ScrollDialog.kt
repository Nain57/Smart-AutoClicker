package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.scroll

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionScrollBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels


class ScrollDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ScrollViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { scrollViewModel() },
    )

    private lateinit var binding: DialogConfigActionScrollBinding

    override fun onCreateView(): ViewGroup {
        binding = DialogConfigActionScrollBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_scroll)
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
        binding.fieldAxis.setOnItemSelectedListener { idx -> viewModel.setAxisByIndex(idx) }
        binding.fieldDistancePercent.setOnTextChanged { t -> t.toIntOrNull()?.let(viewModel::setDistancePercent) }
        binding.fieldDurationMs.setOnTextChanged { t -> t.toLongOrNull()?.let(viewModel::setDuration) }
        binding.fieldStutter.setOnCheckedChangeListener { _, checked -> viewModel.setStutter(checked) }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.name.collect { binding.fieldName.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.axisIndex.collect { binding.fieldAxis.setSelectionSafe(it) } }
                launch { viewModel.distancePercent.collect { binding.fieldDistancePercent.setTextIfDifferent(it ?: 60) } }
                launch { viewModel.duration.collect { binding.fieldDurationMs.setTextIfDifferent(it ?: 350L) } }
                launch { viewModel.stutter.collect { binding.fieldStutter.isChecked = it ?: true } }
                launch { viewModel.canSave.collect { binding.layoutTopBar.buttonSave.isEnabled = it } }
            }
        }
    }

    private fun onSave() { viewModel.saveLastConfig(); listener.onConfirmClicked(); back() }
    private fun onDelete() { viewModel.deleteAction(); listener.onDeleteClicked(); back() }
}