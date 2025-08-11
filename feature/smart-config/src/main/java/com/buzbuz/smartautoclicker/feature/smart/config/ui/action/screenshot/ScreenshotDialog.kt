package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.screenshot

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionScreenshotBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels

class ScreenshotDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ScreenshotViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { screenshotViewModel() },
    )

    private lateinit var binding: DialogConfigActionScreenshotBinding

    override fun onCreateView(): ViewGroup {
        binding = DialogConfigActionScreenshotBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_screenshot)
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
        binding.fieldSavePath.setOnTextChanged { viewModel.setSavePath(it) }

        fun readRect(): Rect? {
            val l = binding.fieldRoiLeft.textAsIntOrNull()
            val t = binding.fieldRoiTop.textAsIntOrNull()
            val w = binding.fieldRoiWidth.textAsIntOrNull()
            val h = binding.fieldRoiHeight.textAsIntOrNull()
            return if (l!=null && t!=null && w!=null && h!=null) Rect(l, t, l+w, t+h) else null
        }
        binding.fieldRoiLeft.setOnTextChanged { viewModel.setRoi(readRect()) }
        binding.fieldRoiTop.setOnTextChanged { viewModel.setRoi(readRect()) }
        binding.fieldRoiWidth.setOnTextChanged { viewModel.setRoi(readRect()) }
        binding.fieldRoiHeight.setOnTextChanged { viewModel.setRoi(readRect()) }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.name.collect { binding.fieldName.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.savePath.collect { binding.fieldSavePath.setTextIfDifferent(it.orEmpty()) } }
                launch { viewModel.roi.collect { r ->
                    if (r!=null) {
                        binding.fieldRoiLeft.setTextIfDifferent(r.left)
                        binding.fieldRoiTop.setTextIfDifferent(r.top)
                        binding.fieldRoiWidth.setTextIfDifferent(r.width())
                        binding.fieldRoiHeight.setTextIfDifferent(r.height())
                    }
                } }
                launch { viewModel.canSave.collect { binding.layoutTopBar.buttonSave.isEnabled = it } }
            }
        }
    }

    private fun onSave() { viewModel.saveLastConfig(); listener.onConfirmClicked(); back() }
    private fun onDelete() { viewModel.deleteAction(); listener.onDeleteClicked(); back() }
}