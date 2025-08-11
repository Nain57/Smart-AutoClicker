
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.DialogDebugReportBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/** Displays the content of the current debug report. */
class DebugReportDialog : OverlayDialog(R.style.AppTheme) {

    /** View model for this dialog. */
    private val viewModel: DebugReportModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogDebugReportBinding
    /** View binding for all views in this content. */
    private lateinit var listBinding: IncludeLoadableListBinding
    /** Adapter for the report */
    private lateinit var reportAdapter: DebugReportAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogDebugReportBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_debug_report)
                buttonSave.visibility = View.GONE
                buttonDismiss.setOnClickListener { back() }
            }
        }

        listBinding = viewBinding.layoutList
        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        reportAdapter = DebugReportAdapter(
            bitmapProvider = viewModel::getConditionBitmap,
            onConditionClicked = ::showConditionReportDialog,
        )
        listBinding.list.adapter = reportAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportItems.collect(::updateReport)
            }
        }
    }

    private fun updateReport(reportItems: List<DebugReportItem>) {
        listBinding.updateState(reportItems)
        reportAdapter.submitList(reportItems)
    }

    private fun showConditionReportDialog(conditionReport: ConditionReport) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = DebugReportConditionDialog(conditionReport),
        )
    }
}