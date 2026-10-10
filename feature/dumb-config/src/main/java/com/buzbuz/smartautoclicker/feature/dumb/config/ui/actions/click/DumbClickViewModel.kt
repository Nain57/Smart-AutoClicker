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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.click

import android.content.Context
import android.graphics.Point

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putClickRepeatCountConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putClickRepeatDelayConfig

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import javax.inject.Inject

class DumbClickViewModel @Inject constructor() : ViewModel() {

    private val _editedDumbClick: MutableStateFlow<DumbAction.DumbClick?> = MutableStateFlow(null)

    /** UI state of the DumbClickDialog. */
    val uiState: StateFlow<DumbClickDialogUiState> = _editedDumbClick
        .map { click -> click?.toUiState() ?: DumbClickDialogUiState() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(3_000),
            DumbClickDialogUiState(),
        )

    fun setEditedDumbClick(click: DumbAction.DumbClick) {
        _editedDumbClick.value = click.copy()
    }

    fun getEditedDumbClick(): DumbAction.DumbClick? =
        _editedDumbClick.value

    fun setName(newName: String) {
        _editedDumbClick.update { click -> click?.copy(name = newName) }
    }

    fun setPressDurationMs(durationMs: Long) {
        _editedDumbClick.update { click -> click?.copy(pressDurationMs = durationMs) }
    }

    fun setRepeatCount(repeatCount: Int) {
        _editedDumbClick.update { click -> click?.copy(repeatCount = repeatCount) }
    }

    fun toggleInfiniteRepeat() {
        _editedDumbClick.update { click -> click?.copy(isRepeatInfinite = !click.isRepeatInfinite) }
    }

    fun setRepeatDelay(delayMs: Long) {
        _editedDumbClick.update { click -> click?.copy(repeatDelayMs = delayMs) }
    }

    fun setPosition(position: Point?) {
        position ?: return
        _editedDumbClick.update { click -> click?.copy(position = position) }
    }

    fun setPositionX(newXValue: String) {
        val newX = newXValue.toIntOrNull() ?: 0
        _editedDumbClick.update { click -> click?.copy(position = Point(newX, click.position.y)) }
    }

    fun setPositionY(newYValue: String) {
        val newY = newYValue.toIntOrNull() ?: 0
        _editedDumbClick.update { click -> click?.copy(position = Point(click.position.x, newY)) }
    }

    fun saveLastConfig(context: Context) {
        _editedDumbClick.value?.let { click ->
            context.getDumbConfigPreferences().edit {
                putClickPressDurationConfig(click.pressDurationMs)
                putClickRepeatCountConfig(click.repeatCount)
                putClickRepeatDelayConfig(click.repeatDelayMs)
            }
        }
    }

    private fun DumbAction.DumbClick.toUiState(): DumbClickDialogUiState =
        DumbClickDialogUiState(
            canBeSaved = isValid(),
            name = name,
            nameError = name.isEmpty(),
            pressDurationMs = pressDurationMs,
            pressDurationError = pressDurationMs <= 0,
            repeatCount = repeatCount,
            repeatCountError = repeatCount <= 0,
            isRepeatInfinite = isRepeatInfinite,
            repeatDelayMs = repeatDelayMs,
            repeatDelayError = !isRepeatDelayValid(),
            position = position,
        )
}
