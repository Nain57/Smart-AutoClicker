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
package com.buzbuz.smartautoclicker.overlays.eventconfig

import android.content.Context

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.Condition

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * View model for the [EventConfigDialog] sub overlays.
 * @param context the Android context.
 */
class ConfigSubOverlayModel(context: Context) : OverlayViewModel(context) {

    /** Backing field for [subOverlayRequest]. */
    private val _subOverlayRequest = MutableStateFlow<SubOverlay>(SubOverlay.None)
    /** The subOverlay currently requested by the user. Changed with [requestSubOverlay], consumed with [consumeRequest] */
    val subOverlayRequest: Flow<SubOverlay> = _subOverlayRequest

    /**
     * Request a new sub overlay.
     * @param overlayType the type of overlay requested.
     */
    fun requestSubOverlay(overlayType: SubOverlay) {
        viewModelScope.launch {
            _subOverlayRequest.emit(overlayType)
        }
    }

    /** Consume the current sub overlay request and replace it by [SubOverlay]. */
    fun consumeRequest() {
        viewModelScope.launch {
            _subOverlayRequest.emit(SubOverlay.None)
        }
    }
}

/** Base class for the type of sub overlay displayable by the [EventConfigDialog]. */
sealed class SubOverlay {

    /** No sub overlay, [EventConfigDialog] is shown. */
    object None : SubOverlay()

    /** Action type selection dialog. */
    object ActionTypeSelection : SubOverlay()

    /** Action copy dialog. */
    object ActionCopy : SubOverlay()

    /**
     * Action config dialog.
     * @param action the action to be configured.
     * @param index the index of the action in the list.
     */
    class ActionConfig(val action: Action, val index: Int = -1) : SubOverlay()

    /** Condition copy dialog. */
    object ConditionCopy : SubOverlay()

    /** Condition capture sub overlay menu. */
    object ConditionCapture : SubOverlay()

    /**
     * Condition config dialog.
     * @param condition the condition to be configured.
     * @param index the index of the condition in the list.
     */
    class ConditionConfig(val condition: Condition, val index: Int = -1) : SubOverlay()
}
