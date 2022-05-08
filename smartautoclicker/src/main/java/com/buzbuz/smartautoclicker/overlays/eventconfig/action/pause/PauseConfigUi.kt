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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.pause

import android.text.Editable
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.databinding.IncludePauseConfigBinding
import com.buzbuz.smartautoclicker.overlays.utils.DurationInputFilter
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Binds the [IncludePauseConfigBinding] to the [PauseConfigModel] using the dialog lifecycle. */
fun IncludePauseConfigBinding.setupPauseUi(
    pauseModel: PauseConfigModel,
    lifecycleOwner: LifecycleOwner,
    lifecycleScope: CoroutineScope,
) {
    actionConfigLayoutPause.visibility = View.VISIBLE

    editPauseDuration.apply {
        setSelectAllOnFocus(true)
        filters = arrayOf(DurationInputFilter())
        addTextChangedListener(object : OnAfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                pauseModel.setPauseDuration(if (!s.isNullOrEmpty()) s.toString().toLong() else null)
            }
        })
    }

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                pauseModel.pauseDuration.collect { duration ->
                    editPauseDuration.apply {
                        setText(duration.toString())
                        setSelection(text.length)
                    }
                }
            }
        }
    }
}