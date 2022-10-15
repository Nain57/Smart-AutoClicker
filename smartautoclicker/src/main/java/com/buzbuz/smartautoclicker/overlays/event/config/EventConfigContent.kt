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
package com.buzbuz.smartautoclicker.overlays.event.config

import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.databinding.ContentEventConfigBinding
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.event.EventDialogViewModel
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class EventConfigContent : NavBarDialogContent() {

    /** View model for the container dialog. */
    private val dialogViewModel: EventDialogViewModel by lazy {
        ViewModelProvider(dialogViewModelStoreOwner).get(EventDialogViewModel::class.java)
    }
    /** View model for this content. */
    private val viewModel: EventConfigViewModel by lazy {
        ViewModelProvider(this).get(EventConfigViewModel::class.java)
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentEventConfigBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setConfiguredEvent(dialogViewModel.configuredEvent)

        viewBinding = ContentEventConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            eventNameInputEditText.apply {
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setEventName(s.toString())
                    }
                })
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventName.collect(::updateEventName) }
            }
        }
    }

    private fun updateEventName(name: String?) {
        viewBinding.eventNameInputEditText.setText(name)
    }
}