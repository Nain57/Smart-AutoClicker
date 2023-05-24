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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings

import android.content.ComponentName

import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemApplicationBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.ActivityDisplayInfo

/** Binds to the provided activity. */
fun ItemApplicationBinding.bind(activity: ActivityDisplayInfo, listener: ((ComponentName) -> Unit)? = null) {
    textApp.text = activity.name
    iconApp.setImageDrawable(activity.icon)

    listener?.let { root.setOnClickListener { it(activity.componentName) } }
}