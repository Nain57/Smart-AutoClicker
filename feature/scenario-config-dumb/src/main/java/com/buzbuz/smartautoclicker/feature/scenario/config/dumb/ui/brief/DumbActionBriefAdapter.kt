/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist.DumbActionDetails
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist.DumbActionDiffUtilCallback

class DumbActionBriefAdapter(
    private val displayMetrics: DisplayMetrics,
    private val actionClickedListener: (DumbActionDetails) -> Unit,
) : ListAdapter<DumbActionDetails, DumbActionBriefViewHolder>(DumbActionDiffUtilCallback) {

    private var orientation: Int = displayMetrics.orientation

    override fun getItemViewType(position: Int): Int = orientation

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DumbActionBriefViewHolder =
        DumbActionBriefViewHolder(LayoutInflater.from(parent.context)
            .inflateDumbActionBriefItemViewBinding(orientation, parent))

    override fun onBindViewHolder(holder: DumbActionBriefViewHolder, position: Int) {
        holder.onBind(getItem(position), actionClickedListener)
    }

    @SuppressLint("NotifyDataSetChanged") // Reload the whole list when the orientation is different
    override fun submitList(list: List<DumbActionDetails>?) {
        if (orientation != displayMetrics.orientation) {
            orientation = displayMetrics.orientation
            notifyDataSetChanged()
            return
        }

        super.submitList(list)
    }
}

/**
 * View holder displaying an action in the [DumbActionBriefAdapter].
 * @param viewBinding the view binding for this item.
 */
class DumbActionBriefViewHolder(
    private val viewBinding: DumbActionBriefItemBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a action item.
     *
     * @param details the action to be represented by this item.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBind(details: DumbActionDetails, actionClickedListener: (DumbActionDetails) -> Unit) {
        viewBinding.apply {
            root.setOnClickListener { actionClickedListener(details) }

            actionName.visibility = View.VISIBLE
            actionTypeIcon.setImageResource(details.icon)
            actionName.text = details.name
            actionDuration.apply {
                text = details.detailsText

                val typedValue = TypedValue()
                val actionColorAttr =
                    if (details.haveError) R.attr.colorError
                    else R.attr.colorOnSurfaceVariant
                root.context.theme.resolveAttribute(actionColorAttr, typedValue, true)
                setTextColor(typedValue.data)
            }

            actionRepeat.apply {
                if (details.repeatCountText != null) {
                    text = details.repeatCountText
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
        }
    }
}

