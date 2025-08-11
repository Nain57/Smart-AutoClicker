
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.copy

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.core.ui.databinding.ItemListHeaderBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.copy.EventCopyModel.EventCopyItem

/**
 * Adapter displaying all events in a list.
 * @param onEventSelected Called when the user presses an event.
 */
class EventCopyAdapter(
    private val onEventSelected: (Event) -> Unit
) : ListAdapter<EventCopyItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when(getItem(position)) {
            is EventCopyItem.Header -> R.layout.item_list_header
            is EventCopyItem.EventItem.Image -> R.layout.item_image_event
            is EventCopyItem.EventItem.Trigger -> R.layout.item_trigger_event
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_list_header -> HeaderViewHolder(
                ItemListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_image_event -> ImageEventViewHolder(
                ItemImageEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_trigger_event -> TriggerEventViewHolder(
                ItemTriggerEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.onBind(getItem(position) as EventCopyItem.Header)
            is ImageEventViewHolder -> holder.onBind(getItem(position) as EventCopyItem.EventItem.Image, onEventSelected)
            is TriggerEventViewHolder -> holder.onBind(getItem(position) as EventCopyItem.EventItem.Trigger, onEventSelected)
        }
    }
}

/** DiffUtil Callback comparing two items when updating the [EventCopyAdapter] list. */
private object DiffUtilCallback: DiffUtil.ItemCallback<EventCopyItem>() {
    override fun areItemsTheSame(oldItem: EventCopyItem, newItem: EventCopyItem): Boolean =
        when {
            oldItem is EventCopyItem.Header && newItem is EventCopyItem.Header -> true
            oldItem is EventCopyItem.EventItem && newItem is EventCopyItem.EventItem ->
                oldItem.uiEvent.event.id == newItem.uiEvent.event.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: EventCopyItem, newItem: EventCopyItem): Boolean = oldItem == newItem
}

/**
 * View holder displaying a header in the [EventCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
class HeaderViewHolder(
    private val viewBinding: ItemListHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: EventCopyItem.Header) {
        viewBinding.textHeader.setText(header.title)
    }
}


class ImageEventViewHolder(private val viewBinding: ItemImageEventBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: EventCopyItem.EventItem.Image, eventClickedListener: (ImageEvent) -> Unit) {
        viewBinding.bind(item.uiEvent, false, eventClickedListener)
    }
}

class TriggerEventViewHolder(private val viewBinding: ItemTriggerEventBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: EventCopyItem.EventItem.Trigger, eventClickedListener: (TriggerEvent) -> Unit) {
        viewBinding.bind(item.uiEvent, eventClickedListener)
    }
}