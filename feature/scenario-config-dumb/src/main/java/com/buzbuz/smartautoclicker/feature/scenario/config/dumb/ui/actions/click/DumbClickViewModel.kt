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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.click

import android.app.Application
import android.content.Context
import android.graphics.Point

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.putClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.putClickRepeatCountConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.putClickRepeatDelayConfig

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

class DumbClickViewModel(application: Application) : AndroidViewModel(application) {

    private val _editedDumbClick: MutableStateFlow<DumbAction.DumbClick?> = MutableStateFlow(null)
    private val editedDumbClick: Flow<DumbAction.DumbClick> = _editedDumbClick.filterNotNull()

    /** Tells if the configured dumb click is valid and can be saved. */
    val isValidDumbClick: Flow<Boolean> = _editedDumbClick
        .map { it != null && it.isValid() }

    /** The name of the click. */
    val name: Flow<String> = editedDumbClick
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = editedDumbClick
        .map { it.name.isEmpty() }

    /** The duration between the press and release of the click in milliseconds. */
    val pressDuration: Flow<String> = editedDumbClick
        .map { it.pressDurationMs.toString() }
        .take(1)
    /** Tells if the press duration value is valid or not. */
    val pressDurationError: Flow<Boolean> = editedDumbClick
        .map { it.pressDurationMs <= 0 }

    /** The number of times to repeat the action. */
    val repeatCount: Flow<String> = editedDumbClick
        .map { it.repeatCount.toString() }
        .take(1)
    /** Tells if the repeat count value is valid or not. */
    val repeatCountError: Flow<Boolean> = editedDumbClick
        .map { it.repeatCount <= 0 }
    /** Tells if the action should be repeated infinitely. */
    val repeatInfiniteState: Flow<Boolean> = editedDumbClick
        .map { it.isRepeatInfinite }
    /** The delay, in ms, between two repeats of the action. */
    val repeatDelay: Flow<String> = editedDumbClick
        .map { it.repeatDelayMs.toString() }
        .take(1)
    /** Tells if the delay is valid or not. */
    val repeatDelayError: Flow<Boolean> = editedDumbClick
        .map { !it.isRepeatDelayValid() }

    /** Subtext for the position selector. */
    val clickPositionText: Flow<String> = editedDumbClick
        .map { dumbClick ->
            application.getString(
                R.string.item_desc_dumb_click_on_position,
                dumbClick.position.x,
                dumbClick.position.y,
            )
        }

    fun setEditedDumbClick(click: DumbAction.DumbClick) {
        _editedDumbClick.value = click.copy()
    }

    fun getEditedDumbClick(): DumbAction.DumbClick? =
        _editedDumbClick.value

    fun setName(newName: String) {
        _editedDumbClick.value = _editedDumbClick.value?.copy(name = newName)
    }

    fun setPressDurationMs(durationMs: Long) {
        _editedDumbClick.value = _editedDumbClick.value?.copy(pressDurationMs = durationMs)
    }

    fun setRepeatCount(repeatCount: Int) {
        _editedDumbClick.value = _editedDumbClick.value?.copy(repeatCount = repeatCount)
    }

    fun toggleInfiniteRepeat() {
        val currentValue = _editedDumbClick.value?.isRepeatInfinite ?: return
        _editedDumbClick.value = _editedDumbClick.value?.copy(isRepeatInfinite = !currentValue)
    }

    fun setRepeatDelay(delayMs: Long) {
        _editedDumbClick.value = _editedDumbClick.value?.copy(repeatDelayMs = delayMs)
    }

    fun setPosition(position: Point?) {
        position ?: return
        _editedDumbClick.value = _editedDumbClick.value?.copy(position = position)
    }

    fun saveLastConfig(context: Context) {
        _editedDumbClick.value?.let { click ->
            context.getDumbConfigPreferences()
                .edit()
                .putClickPressDurationConfig(click.pressDurationMs)
                .putClickRepeatCountConfig(click.repeatCount)
                .putClickRepeatDelayConfig(click.repeatDelayMs)
                .apply()
        }
    }
}