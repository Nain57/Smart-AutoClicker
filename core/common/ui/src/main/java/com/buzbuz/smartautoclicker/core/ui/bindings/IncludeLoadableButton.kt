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
package com.buzbuz.smartautoclicker.core.ui.bindings

import android.view.View

import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableButtonBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableButtonOutlinedBinding

import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator


fun IncludeLoadableButtonBinding.setOnClickListener(onClick: () -> Unit): Unit =
    button.setOnClickListener { onClick() }
fun IncludeLoadableButtonOutlinedBinding.setOnClickListener(onClick: () -> Unit): Unit =
    button.setOnClickListener { onClick() }

fun IncludeLoadableButtonBinding.setState(state: LoadableButtonState): Unit =
    setState(button, loading, state)
fun IncludeLoadableButtonOutlinedBinding.setState(state: LoadableButtonState): Unit =
    setState(button, loading, state)

private fun setState(button: MaterialButton, progress: CircularProgressIndicator, state: LoadableButtonState): Unit =
    when (state) {
        LoadableButtonState.Loading -> {
            button.alpha = DISABLED_ITEM_ALPHA
            button.text = ""
            progress.show()
        }

        is LoadableButtonState.Loaded -> {
            button.text = state.text
            progress.visibility = View.GONE

            when (state) {
                is LoadableButtonState.Loaded.Enabled -> {
                    button.alpha = ENABLED_ITEM_ALPHA
                    button.isEnabled = true
                }

                is LoadableButtonState.Loaded.Disabled -> {
                    button.alpha = DISABLED_ITEM_ALPHA
                    button.isEnabled = false
                }
            }
        }
    }

sealed class LoadableButtonState {
    data object Loading : LoadableButtonState()
    sealed class Loaded : LoadableButtonState() {
        abstract val text: String
        data class Enabled(override val text: String) : Loaded()
        data class Disabled(override val text: String) : Loaded()
    }
}

private const val ENABLED_ITEM_ALPHA = 1f
private const val DISABLED_ITEM_ALPHA = 0.5f