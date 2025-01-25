/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.scenarios.list.adapter

import android.graphics.Bitmap
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState
import com.buzbuz.smartautoclicker.core.base.extensions.setLeftCompoundDrawable
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.databinding.ItemDumbScenarioBinding
import com.buzbuz.smartautoclicker.databinding.ItemEmptyScenarioBinding
import com.buzbuz.smartautoclicker.databinding.ItemSmartScenarioBinding
import kotlinx.coroutines.Job

class EmptyScenarioHolder(
    private val viewBinding: ItemEmptyScenarioBinding,
    private val startScenarioListener: ((ScenarioListUiState.Item.ScenarioItem.Empty) -> Unit),
    private val deleteScenarioListener: ((ScenarioListUiState.Item.ScenarioItem.Empty) -> Unit),
): RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(scenarioItem: ScenarioListUiState.Item.ScenarioItem.Empty) = viewBinding.apply {
        scenarioName.text = scenarioItem.displayName
        scenarioName.setLeftCompoundDrawable(
            if (scenarioItem.scenario is DumbScenario) R.drawable.ic_dumb
            else R.drawable.ic_smart
        )

        buttonStart.setOnClickListener { startScenarioListener(scenarioItem) }
        buttonDelete.setOnClickListener { deleteScenarioListener(scenarioItem) }
    }
}

/** ViewHolder for the [ScenarioAdapter]. */
class DumbScenarioViewHolder(
    private val viewBinding: ItemDumbScenarioBinding,
    private val startScenarioListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val expandCollapseListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val exportClickListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val copyClickedListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val deleteScenarioListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(scenarioItem: ScenarioListUiState.Item.ScenarioItem.Valid.Dumb) = viewBinding.apply {
        scenarioName.text = scenarioItem.displayName

        if (scenarioItem.showExportCheckbox) {
            buttonExpandCollapse.visibility = View.INVISIBLE
            buttonExpandCollapse.isEnabled = false
            buttonExport.apply {
                visibility = View.VISIBLE
                isChecked = scenarioItem.checkedForExport
            }
            topDivider.visibility = View.GONE
            root.setOnClickListener { exportClickListener(scenarioItem) }

        } else {
            buttonExpandCollapse.visibility = View.VISIBLE
            buttonExpandCollapse.isEnabled = true
            buttonExport.visibility = View.GONE
            topDivider.visibility = View.VISIBLE
            root.setOnClickListener { startScenarioListener(scenarioItem) }
        }

        if (!scenarioItem.showExportCheckbox && scenarioItem.expanded) {
            scenarioDetails.visibility = View.VISIBLE
            buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_up)
            clickCount.text = scenarioItem.clickCount.toString()
            swipeCount.text = scenarioItem.swipeCount.toString()
            pauseCount.text = scenarioItem.pauseCount.toString()
            repeatLimit.text = scenarioItem.repeatText
            durationLimit.text = scenarioItem.maxDurationText
        } else {
            buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_down)
            scenarioDetails.visibility = View.GONE
        }

        buttonCopy.setOnClickListener { copyClickedListener(scenarioItem) }
        buttonExpandCollapse.setOnClickListener { expandCollapseListener(scenarioItem) }
        buttonDelete.setOnClickListener { deleteScenarioListener(scenarioItem) }
        buttonExport.setOnClickListener { exportClickListener(scenarioItem) }
    }
}

/** ViewHolder for the [ScenarioAdapter]. */
class SmartScenarioViewHolder(
    private val viewBinding: ItemSmartScenarioBinding,
    bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val startScenarioListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val expandCollapseListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val exportClickListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val copyClickedListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
    private val deleteScenarioListener: ((ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit),
) : RecyclerView.ViewHolder(viewBinding.root) {

    private val eventsAdapter = ScenarioEventsAdapter(bitmapProvider)

    init {
        viewBinding.listEvent.adapter = eventsAdapter
    }

    fun onBind(scenarioItem: ScenarioListUiState.Item.ScenarioItem.Valid.Smart) = viewBinding.apply {
        scenarioName.text = scenarioItem.displayName

        if (scenarioItem.showExportCheckbox) {
            buttonExpandCollapse.visibility = View.INVISIBLE
            buttonExpandCollapse.isEnabled = false
            buttonExport.apply {
                visibility = View.VISIBLE
                isChecked = scenarioItem.checkedForExport
            }
            topDivider.visibility = View.GONE
            root.setOnClickListener { exportClickListener(scenarioItem) }
        } else {
            buttonExpandCollapse.visibility = View.VISIBLE
            buttonExpandCollapse.isEnabled = true
            buttonExport.visibility = View.GONE
            topDivider.visibility = View.VISIBLE
            root.setOnClickListener { startScenarioListener(scenarioItem) }
        }

        if (!scenarioItem.showExportCheckbox && scenarioItem.expanded) {
            scenarioDetails.visibility = View.VISIBLE
            buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_up)
            detectionQuality.text = scenarioItem.detectionQuality.toString()
            triggerEventCount.text = scenarioItem.triggerEventCount.toString()

            eventsAdapter.submitList(scenarioItem.eventsItems)
            if (scenarioItem.eventsItems.isEmpty()) {
                listEvent.visibility = View.GONE
                noImageEvents.visibility = View.VISIBLE
            } else {
                listEvent.visibility = View.VISIBLE
                noImageEvents.visibility = View.GONE
            }
        } else {
            buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_down)
            scenarioDetails.visibility = View.GONE
        }

        buttonCopy.setOnClickListener { copyClickedListener(scenarioItem) }
        buttonExpandCollapse.setOnClickListener { expandCollapseListener(scenarioItem) }
        buttonDelete.setOnClickListener { deleteScenarioListener(scenarioItem) }
        buttonExport.setOnClickListener { exportClickListener(scenarioItem) }
    }
}