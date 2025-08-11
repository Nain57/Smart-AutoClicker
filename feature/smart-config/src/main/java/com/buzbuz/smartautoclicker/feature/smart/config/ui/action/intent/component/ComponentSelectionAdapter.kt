
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.component

import android.content.ComponentName
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.android.application.AndroidApplicationInfo
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemComponentNameBinding


/**
 * Adapter for the list of applications.
 * @param onApplicationSelected listener on user click on an application.
 */
class ComponentSelectionAdapter(
    private val onApplicationSelected: (ComponentName) -> Unit,
) : ListAdapter<AndroidApplicationInfo, ComponentViewHolder>(ComponentDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComponentViewHolder =
        ComponentViewHolder(
            ItemComponentNameBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onApplicationSelected,
        )

    override fun onBindViewHolder(holder: ComponentViewHolder, position: Int) =
        holder.onBind(getItem(position))
}

/** DiffUtil Callback comparing two lists when updating the [ComponentSelectionAdapter]. */
private object ComponentDiffUtilCallback: DiffUtil.ItemCallback<AndroidApplicationInfo>() {
    override fun areItemsTheSame(oldItem: AndroidApplicationInfo, newItem: AndroidApplicationInfo):
            Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: AndroidApplicationInfo, newItem: AndroidApplicationInfo):
            Boolean = oldItem == newItem
}

/**
 * ViewHolder for an application.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param onApplicationSelected called when the user select an application.
 */
class ComponentViewHolder(
    private val viewBinding: ItemComponentNameBinding,
    private val onApplicationSelected: (ComponentName) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    /** Binds this view holder views to the provided activity. */
    fun onBind(activity: AndroidApplicationInfo) {
        viewBinding.apply {
            iconApp.setImageDrawable(activity.icon)
            appName.text = activity.name
            componentName.text = activity.componentName.packageName

            root.setOnClickListener { onApplicationSelected(activity.componentName) }
        }
    }
}