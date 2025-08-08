
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.views.areaselector.AreaSelectorView
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayValidationMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import kotlinx.coroutines.launch

class ImageConditionAreaSelectorMenu(
    private val onAreaSelected: (Rect) -> Unit
) : OverlayMenu() {

    /** The view model for this dialog. */
    private val viewModel: ImageConditionAreaSelectorViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionAreaSelectorViewModel() },
    )

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayValidationMenuBinding
    /** The view displaying selector for the area. */
    private lateinit var selectorView: AreaSelectorView

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        selectorView = AreaSelectorView(context, displayConfigManager)
        viewBinding = OverlayValidationMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateOverlayView(): View = selectorView

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.initialArea.collect { selectorState ->
                    selectorView.setSelection(selectorState.initialArea, selectorState.minimalArea)
                }
            }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
        }
    }

    /** Called when the user press the confirmation button. */
    private fun onConfirm() {
        onAreaSelected(selectorView.getSelection())
        back()
    }

    /** Called when the user press the cancel button. */
    private fun onCancel() {
        back()
    }
}