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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.actionbrief

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayViewActionBriefLandBinding
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayViewActionBriefPortBinding
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionBriefView


class ActionBriefBinding private constructor(
    val root: View,
    val viewBrief: ActionBriefView,
    val layoutActionList: View,
    val listActions: RecyclerView,
    val textActionIndex: TextView,
    val buttonPrevious: Button,
    val buttonNext: Button,
    val emptyScenarioCard: View,
    val emptyScenarioText: TextView,
) {

    companion object {

        fun inflate(inflater: LayoutInflater, orientation: Int) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ActionBriefBinding(OverlayViewActionBriefPortBinding.inflate(inflater))
            else
                ActionBriefBinding(OverlayViewActionBriefLandBinding.inflate(inflater))
    }

    constructor(binding: OverlayViewActionBriefPortBinding) : this(
        root = binding.root,
        viewBrief = binding.viewBrief,
        layoutActionList = binding.layoutActionList,
        listActions = binding.listActions,
        textActionIndex = binding.textActionIndex,
        buttonPrevious = binding.buttonPrevious,
        buttonNext = binding.buttonNext,
        emptyScenarioCard = binding.emptyScenarioCard,
        emptyScenarioText = binding.textEmptyScenario,
    )

    constructor(binding: OverlayViewActionBriefLandBinding) : this(
        root = binding.root,
        viewBrief = binding.viewBrief,
        layoutActionList = binding.layoutActionList,
        listActions = binding.listActions,
        textActionIndex = binding.textActionIndex,
        buttonPrevious = binding.buttonPrevious,
        buttonNext = binding.buttonNext,
        emptyScenarioCard = binding.emptyScenarioCard,
        emptyScenarioText = binding.textEmptyScenario,
    )
}