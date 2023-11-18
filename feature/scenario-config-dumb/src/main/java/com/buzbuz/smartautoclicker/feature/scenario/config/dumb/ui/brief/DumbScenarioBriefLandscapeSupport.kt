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

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionBriefView
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.ItemDumbActionBriefBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.ItemDumbActionBriefLandBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbScenarioBriefBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbScenarioBriefLandBinding


fun LayoutInflater.inflateDumbScenarioBriefViewBinding(orientation: Int) =
    if (orientation == Configuration.ORIENTATION_PORTRAIT)
        DumbScenarioBriefViewBinding(OverlayDumbScenarioBriefBinding.inflate(this))
    else
        DumbScenarioBriefViewBinding(OverlayDumbScenarioBriefLandBinding.inflate(this))

fun LayoutInflater.inflateDumbActionBriefItemViewBinding(orientation: Int, parent: ViewGroup) =
    if (orientation == Configuration.ORIENTATION_PORTRAIT)
        DumbActionBriefItemBinding(ItemDumbActionBriefBinding.inflate(this, parent, false))
    else
        DumbActionBriefItemBinding(ItemDumbActionBriefLandBinding.inflate(this, parent, false))

class DumbScenarioBriefViewBinding private constructor(
    val root: View,
    val viewDumbBrief: ActionBriefView,
    val layoutActionList: View,
    val listDumbActions: RecyclerView,
    val textDumbActionIndex: TextView,
    val buttonPrevious: Button,
    val buttonNext: Button,
    val emptyScenarioCard: View,
) {

    constructor(binding: OverlayDumbScenarioBriefBinding) : this(
        root = binding.root,
        viewDumbBrief = binding.viewDumbBrief,
        layoutActionList = binding.layoutActionList,
        listDumbActions = binding.listDumbActions,
        textDumbActionIndex = binding.textDumbActionIndex,
        buttonPrevious = binding.buttonPrevious,
        buttonNext = binding.buttonNext,
        emptyScenarioCard = binding.emptyScenarioCard,
    )

    constructor(binding: OverlayDumbScenarioBriefLandBinding) : this(
        root = binding.root,
        viewDumbBrief = binding.viewDumbBrief,
        layoutActionList = binding.layoutActionList,
        listDumbActions = binding.listDumbActions,
        textDumbActionIndex = binding.textDumbActionIndex,
        buttonPrevious = binding.buttonPrevious,
        buttonNext = binding.buttonNext,
        emptyScenarioCard = binding.emptyScenarioCard,
    )
}

class DumbActionBriefItemBinding private constructor(
    val root: View,
    val actionTypeIcon: ImageView,
    val actionName: TextView,
    val actionDuration: TextView,
    val actionRepeat: TextView,
) {

    constructor(binding: ItemDumbActionBriefBinding) : this(
        root = binding.root,
        actionTypeIcon = binding.actionTypeIcon,
        actionName = binding.actionName,
        actionDuration = binding.actionDuration,
        actionRepeat = binding.actionRepeat,
    )

    constructor(binding: ItemDumbActionBriefLandBinding) : this(
        root = binding.root,
        actionTypeIcon = binding.actionTypeIcon,
        actionName = binding.actionName,
        actionDuration = binding.actionDuration,
        actionRepeat = binding.actionRepeat,
    )
}

