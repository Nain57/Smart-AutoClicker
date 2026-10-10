/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.offset

import android.graphics.PointF

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update

import javax.inject.Inject


class GestureOffsetViewModel @Inject constructor() : ViewModel() {

    private val editedOffset = MutableStateFlow<GesturePosition.Coordinates?>(null)
    private val updateSource = MutableStateFlow(GestureOffsetUpdateSource.OTHER)

    val uiState: Flow<GestureOffsetUiState> =
        combine(editedOffset, updateSource) { offset, source ->
            offset ?: return@combine null
            GestureOffsetUiState(
                xState = offset.x.toValueUiState(),
                yState = offset.y.toValueUiState(),
                updateFrom = source,
            )
        }.filterNotNull()

    fun initialize(currentOffset: GesturePosition.Coordinates) {
        if (editedOffset.value != null) return
        editedOffset.value = currentOffset
    }

    fun setXLiteral(value: Int) {
        editedOffset.update { it?.copy(x = GestureCoordinatesValue.Literal(value)) }
        updateSource.value = GestureOffsetUpdateSource.TEXT_INPUT
    }

    fun setXCounter(counterName: String) {
        editedOffset.update { it?.copy(x = GestureCoordinatesValue.Counter(counterName)) }
        updateSource.value = GestureOffsetUpdateSource.OTHER
    }

    fun setYLiteral(value: Int) {
        editedOffset.update { it?.copy(y = GestureCoordinatesValue.Literal(value)) }
        updateSource.value = GestureOffsetUpdateSource.TEXT_INPUT
    }

    fun setYCounter(counterName: String) {
        editedOffset.update { it?.copy(y = GestureCoordinatesValue.Counter(counterName)) }
        updateSource.value = GestureOffsetUpdateSource.OTHER
    }

    fun setOffsetFromView(pointF: PointF) {
        editedOffset.update { current ->
            current?.copy(
                x = GestureCoordinatesValue.Literal(pointF.x.toInt()),
                y = GestureCoordinatesValue.Literal(pointF.y.toInt()),
            )
        }
        updateSource.value = GestureOffsetUpdateSource.VIEW
    }

    fun getCurrentOffset(): GesturePosition.Coordinates? = editedOffset.value

    private fun GestureCoordinatesValue.toValueUiState(): GestureOffsetValueUiState =
        when (this) {
            is GestureCoordinatesValue.Literal -> GestureOffsetValueUiState.Literal(value.toString())
            is GestureCoordinatesValue.Counter -> GestureOffsetValueUiState.Counter("{$counterName}")
        }
}
