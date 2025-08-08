
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.CopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [CopyDialog] implementation for displaying the whole list of conditions for a copy.
 *
 * @param onConditionSelected the listener called when the user select a Condition.
 */
class ConditionCopyDialog(
    private val onConditionSelected: (Condition) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme)  {

    /** View model for this content. */
    private val viewModel: ConditionCopyModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { conditionCopyViewModel() },
    )

    /** Adapter displaying the list of conditions. */
    private lateinit var conditionAdapter: ConditionCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_condition_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        conditionAdapter = ConditionCopyAdapter(
            conditionClickedListener = { selectedCondition ->
                back()
                onConditionSelected(selectedCondition)
            },
            bitmapProvider = { bitmap, onLoaded ->
                viewModel.getConditionBitmap(bitmap, onLoaded)
            },
        )

        viewBinding.layoutLoadableList.list.apply {
            adapter = conditionAdapter

            layoutManager = GridLayoutManager(context, 2).apply {
                spanSizeLookup = conditionAdapter.spanSizeLookup
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditionList.collect(::updateConditionList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun updateConditionList(newItems: List<ConditionCopyModel.ConditionCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newItems)
        conditionAdapter.submitList(if (newItems == null) ArrayList() else ArrayList(newItems))
    }
}