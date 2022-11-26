/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.base.dialog

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.baseui.OverlayController

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

open class NavigationViewModel(application: Application) : AndroidViewModel(application) {

    /** Backing field for [subOverlayRequest]. */
    private val _subOverlayRequest = MutableStateFlow<NavigationRequest?>(null)
    /** The subOverlay currently requested by the user. Changed with [requestSubOverlay], consumed with [consumeRequest] */
    val subOverlayRequest: Flow<NavigationRequest?> = _subOverlayRequest

    /**
     * Request a new sub overlay.
     * @param request the type of overlay requested.
     */
    fun requestSubOverlay(request: NavigationRequest) {
        viewModelScope.launch {
            _subOverlayRequest.emit(request)
        }
    }

    /** Consume the current sub overlay request and replace it by [SubOverlay]. */
    fun consumeRequest() {
        viewModelScope.launch {
            _subOverlayRequest.emit(null)
        }
    }
}

class NavigationRequest(
    val overlay: OverlayController,
    val hideCurrent: Boolean = false,
)