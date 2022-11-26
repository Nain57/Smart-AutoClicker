/*
 * Copyright (C) 2022 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.debugging.report

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogDebugReportConditionBinding
import com.buzbuz.smartautoclicker.overlays.base.bindings.setValues

import com.google.android.material.bottomsheet.BottomSheetDialog

/** Dialog displaying the full debug report for a condition. */
class DebugReportConditionDialog(
    context: Context,
    private val conditionReport: ConditionReport,
): OverlayDialogController(context, R.style.AppTheme) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogDebugReportConditionBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogDebugReportConditionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.text = conditionReport.condition.name
                buttonSave.visibility = View.GONE
                buttonDismiss.setOnClickListener { destroy() }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewBinding.apply {
            rootTriggerCount.apply {
                setValues(
                    R.string.dialog_debug_report_condition_detected_count,
                    conditionReport.matchCount,
                    R.string.dialog_debug_report_condition_processing_count,
                    conditionReport.processingCount,
                )
            }

            rootProcessingTiming.setValues(
                R.string.dialog_debug_report_timing_title,
                conditionReport.minProcessingDuration,
                conditionReport.avgProcessingDuration,
                conditionReport.maxProcessingDuration,
            )

            rootConfidenceRate.setValues(
                R.string.dialog_debug_report_confidence_title,
                conditionReport.minConfidence,
                conditionReport.avgConfidence,
                conditionReport.maxConfidence,
            )
        }
    }
}