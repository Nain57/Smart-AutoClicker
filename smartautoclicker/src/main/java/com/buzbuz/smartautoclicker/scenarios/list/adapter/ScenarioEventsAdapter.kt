
package com.buzbuz.smartautoclicker.scenarios.list.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemEventCardBinding
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState.Item.ScenarioItem.Valid.Smart.EventItem

import kotlinx.coroutines.Job

class ScenarioEventsAdapter(
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<EventItem, EventCardViewHolder>(EventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventCardViewHolder =
        EventCardViewHolder(
            ItemEventCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
        )

    override fun onBindViewHolder(holder: EventCardViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    override fun onViewRecycled(holder: EventCardViewHolder) {
        holder.onUnbind()
    }
}

/** DiffUtil Callback comparing two EventItem when updating the [ScenarioEventsAdapter] list. */
object EventDiffUtilCallback: DiffUtil.ItemCallback<EventItem>() {
    override fun areItemsTheSame(oldItem: EventItem, newItem: EventItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: EventItem, newItem: EventItem): Boolean =
        oldItem == newItem
}

class EventCardViewHolder(
    private val viewBinding: ItemEventCardBinding,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** The job for the condition bitmap loading. */
    private var bitmapJob: Job? = null

    fun onBind(item: EventItem) {
        viewBinding.apply {
            eventName.text = item.eventName
            eventActionsCount.text = item.actionsCount.toString()
            eventConditionsCount.text = item.conditionsCount.toString()

            if (item.firstCondition == null) {
                setErrorBitmap()
                return
            }

            bitmapJob = bitmapProvider(item.firstCondition) { bitmap ->
                if (bitmap != null) {
                    conditionImage.setImageBitmap(bitmap)
                } else {
                   setErrorBitmap()
                }
            }
        }
    }

    fun onUnbind() {
        bitmapJob?.cancel()
        bitmapJob = null
    }

    private fun setErrorBitmap() {
        viewBinding.conditionImage.setImageDrawable(
            ContextCompat.getDrawable(viewBinding.root.context, R.drawable.ic_cancel)?.apply {
                setTint(Color.RED)
            }
        )
    }
}