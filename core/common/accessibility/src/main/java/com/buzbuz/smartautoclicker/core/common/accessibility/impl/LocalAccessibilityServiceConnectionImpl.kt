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
package com.buzbuz.smartautoclicker.core.common.accessibility.impl

import com.buzbuz.smartautoclicker.core.common.accessibility.domain.LocalAccessibilityService
import com.buzbuz.smartautoclicker.core.common.accessibility.domain.LocalAccessibilityServiceConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAccessibilityServiceConnectionImpl @Inject constructor() : LocalAccessibilityServiceConnection {

    private val _localServiceFlow: MutableStateFlow<LocalAccessibilityService?> = MutableStateFlow(null)
    override val localServiceFlow: StateFlow<LocalAccessibilityService?> = _localServiceFlow

    override fun onAccessibilityServiceStarted(service: LocalAccessibilityService) {
        _localServiceFlow.update { service }
    }

    override fun onAccessibilityServiceStopped() {
        _localServiceFlow.update { null }
    }

    override fun isServiceStarted(): Boolean = localServiceFlow.value != null
    override fun getLocalService(): LocalAccessibilityService? = localServiceFlow.value
}