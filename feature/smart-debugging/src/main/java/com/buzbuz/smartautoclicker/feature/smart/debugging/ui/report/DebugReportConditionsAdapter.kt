
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemDebugReportConditionBinding

import kotlinx.coroutines.Job

/** Adapter for the debug condition reports displayed in a event debug report item. */
class DebugReportConditionsAdapter(
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val conditionClickedListener: (ConditionReport) -> Unit,
) : ListAdapter<ConditionReport, ConditionDebugInfoViewHolder>(ConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ConditionDebugInfoViewHolder(
            ItemDebugReportConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
            conditionClickedListener,
        )

    override fun onBindViewHolder(holder: ConditionDebugInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/** DiffUtil Callback comparing two items when updating the [DebugReportConditionsAdapter] list. */
private object ConditionDiffUtilCallback: DiffUtil.ItemCallback<ConditionReport>() {

    override fun areItemsTheSame(oldItem: ConditionReport, newItem: ConditionReport): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ConditionReport, newItem: ConditionReport): Boolean = oldItem == newItem
}

/** ViewHolder for a condition report. */
class ConditionDebugInfoViewHolder(
    private val viewBinding: ItemDebugReportConditionBinding,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionClicked: (ConditionReport) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(conditionReport: ConditionReport) {
        viewBinding.apply {
            root.setOnClickListener { onConditionClicked(conditionReport) }

            conditionName.text = conditionReport.condition.name
            conditionTriggered.text = itemView.context.resources.getString(
                R.string.item_title_debug_report_trigger_processed,
                conditionReport.matchCount,
                conditionReport.processingCount,
            )

            bitmapProvider(conditionReport.condition) { bitmap ->
                if (bitmap != null) {
                    conditionImage.setImageBitmap(bitmap)
                } else {
                    conditionImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_cancel)?.apply {
                            setTint(Color.RED)
                        }
                    )
                }
            }
        }
    }
}