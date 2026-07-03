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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain

import android.graphics.Rect
import android.view.View
import android.widget.EditText
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.ViewPositioningType
import kotlinx.coroutines.flow.StateFlow

interface MonitoredViewsManager {

    fun attach(
        type: MonitoredViewType,
        monitoredView: View,
        positioningType: ViewPositioningType = ViewPositioningType.SCREEN,
    )

    fun detach(type: MonitoredViewType)

    fun notifyClick(type: MonitoredViewType)

    fun getViewPosition(type: MonitoredViewType): StateFlow<Rect>?

    fun performClick(type: MonitoredViewType): Boolean
}