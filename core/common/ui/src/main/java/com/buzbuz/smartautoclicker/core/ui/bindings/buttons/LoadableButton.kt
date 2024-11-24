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
package com.buzbuz.smartautoclicker.core.ui.bindings.buttons

import android.view.View

import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableButtonBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableButtonOutlinedBinding

import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator

/** Configuration for the button represented by [IncludeLoadableButtonBinding] & [IncludeLoadableButtonOutlinedBinding]. */
sealed class LoadableButtonState {

    /** The text of the button. */
    abstract val text: String

    /** The button is in loading state, the text is hidden and the progress spinner is shown. */
    data class Loading(override val text: String = "") : LoadableButtonState()

    /** The button is in loaded state, the text is shown and the progress spinner is hidden. */
    sealed class Loaded : LoadableButtonState() {

        /** The button can be clicked. */
        data class Enabled(override val text: String) : Loaded()
        /** The button can't be clicked and the text is dimmed. */
        data class Disabled(override val text: String) : Loaded()
    }
}


fun IncludeLoadableButtonBinding.setState(state: LoadableButtonState): Unit =
    setState(button, loading, state)

fun IncludeLoadableButtonBinding.setOnClickListener(onClick: () -> Unit): Unit =
    button.setOnClickListener { onClick() }
fun IncludeLoadableButtonOutlinedBinding.setOnClickListener(onClick: () -> Unit): Unit =
    button.setOnClickListener { onClick() }

private fun setState(button: MaterialButton, progress: CircularProgressIndicator, state: LoadableButtonState): Unit =
    when (state) {
        is LoadableButtonState.Loading -> {
            button.alpha = DISABLED_ITEM_ALPHA
            button.text = state.text
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

private const val ENABLED_ITEM_ALPHA = 1f
private const val DISABLED_ITEM_ALPHA = 0.5f