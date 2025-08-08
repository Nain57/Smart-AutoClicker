
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition

import kotlinx.coroutines.Job


internal class EventImageConditionsAdapter(
    private val itemClickedListener: (index: Int) -> Unit,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<UiImageCondition, EventImageConditionViewHolder>(ImageConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventImageConditionViewHolder =
        EventImageConditionViewHolder(
            ItemImageConditionListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
            itemClickedListener,
        )

    override fun onBindViewHolder(holder: EventImageConditionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    override fun onViewRecycled(holder: EventImageConditionViewHolder) {
        holder.onUnbind()
    }
}

internal class EventImageConditionViewHolder (
    private val viewBinding: ItemImageConditionListBinding,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val itemClickedListener: (index: Int) -> Unit,
): ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    fun onBind(condition: UiImageCondition) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.cardImageCondition.bind(condition, bitmapProvider) {
            itemClickedListener(bindingAdapterPosition)
        }
    }

    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}

internal object ImageConditionDiffUtilCallback: DiffUtil.ItemCallback<UiImageCondition>() {
    override fun areItemsTheSame(oldItem: UiImageCondition, newItem: UiImageCondition): Boolean =
        oldItem.condition.id == newItem.condition.id
    override fun areContentsTheSame(oldItem: UiImageCondition, newItem: UiImageCondition): Boolean =
        oldItem == newItem
}