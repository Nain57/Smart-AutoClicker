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
package com.buzbuz.smartautoclicker.core.settings

import kotlinx.coroutines.flow.Flow


interface SettingsRepository {

    val isLegacyActionUiEnabledFlow: Flow<Boolean>
    fun isLegacyActionUiEnabled(): Boolean
    fun toggleLegacyActionUi()

    val isLegacyNotificationUiEnabledFlow: Flow<Boolean>
    fun isLegacyNotificationUiEnabled(): Boolean
    fun toggleLegacyNotificationUi()

    val isEntireScreenCaptureForcedFlow: Flow<Boolean>
    fun isEntireScreenCaptureForced(): Boolean
    fun toggleForceEntireScreenCapture()
}