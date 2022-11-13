/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.config.action.intent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.R

import com.buzbuz.smartautoclicker.databinding.ContentIntentConfigSimpleBinding
import com.buzbuz.smartautoclicker.overlays.base.bindings.bind
import com.buzbuz.smartautoclicker.overlays.base.bindings.setError
import com.buzbuz.smartautoclicker.overlays.base.bindings.setLabel
import com.buzbuz.smartautoclicker.overlays.base.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.overlays.base.bindings.setText
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.config.action.intent.activities.ActivitySelectionDialog

import kotlinx.coroutines.launch

class SimpleIntentContent : NavBarDialogContent() {

    /** View model for the container dialog. */
    private val dialogViewModel: IntentViewModel by lazy {
        ViewModelProvider(dialogController).get(IntentViewModel::class.java)
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentIntentConfigSimpleBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentIntentConfigSimpleBinding.inflate(LayoutInflater.from(context)).apply {
            selectApplicationButton.setOnClickListener { showApplicationSelectionDialog() }
            selectedApplicationLayout.root.setOnClickListener { showApplicationSelectionDialog() }

            editNameLayout.apply {
                setLabel(R.string.dialog_event_config_name_title)
                setOnTextChangedListener { dialogViewModel.setName(it.toString()) }
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.name.collect(::updateClickName) }
                launch { dialogViewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { dialogViewModel.activityInfo.collect(::updateActivityInfo) }
            }
        }
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateActivityInfo(activityInfo: ActivityDisplayInfo?) {
        viewBinding.apply {
            if (activityInfo == null) {
                selectedApplicationLayout.root.visibility = View.GONE
                selectApplicationButton.visibility = View.VISIBLE
            } else {
                selectedApplicationLayout.root.visibility = View.VISIBLE
                selectedApplicationLayout.bind(activityInfo)
                selectApplicationButton.visibility = View.GONE
            }
        }
    }

    private fun showApplicationSelectionDialog() {
        dialogController.showSubOverlay(
            overlayController = ActivitySelectionDialog(
                context = context,
                onApplicationSelected = { componentName ->
                    dialogViewModel.setActivitySelected(componentName)
                }
            ),
            false,
        )
    }
}