
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog

import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.DialogDebugReportConditionBinding

import com.google.android.material.bottomsheet.BottomSheetDialog

/** Dialog displaying the full debug report for a condition. */
class DebugReportConditionDialog(private val conditionReport: ConditionReport) : OverlayDialog(R.style.AppTheme) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogDebugReportConditionBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogDebugReportConditionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.text = conditionReport.condition.name
                buttonSave.visibility = View.GONE
                buttonDismiss.setOnClickListener { back() }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewBinding.apply {
            rootTriggerCount.apply {
                setValues(
                    R.string.section_title_report_condition_detected_count,
                    conditionReport.matchCount,
                    R.string.section_title_report_condition_processing_count,
                    conditionReport.processingCount,
                )
            }

            rootProcessingTiming.setValues(
                R.string.section_title_report_timing_title,
                conditionReport.minProcessingDuration,
                conditionReport.avgProcessingDuration,
                conditionReport.maxProcessingDuration,
            )

            rootConfidenceRate.setValues(
                R.string.section_title_report_confidence_title,
                conditionReport.minConfidence,
                conditionReport.avgConfidence,
                conditionReport.maxConfidence,
            )
        }
    }
}