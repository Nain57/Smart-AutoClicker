/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.gradle.convention.model

import com.buzbuz.gradle.convention.plugins.getPropertyStringValue
import com.buzbuz.gradle.convention.plugins.toTypedValue
import org.gradle.api.Project

class BuildParameter<T : Any>(
    internal val rootProject: Project,
    internal val name: String,
    defaultValue: T,
) {
    val stringValue: String =
        rootProject.getPropertyStringValue(name, defaultValue)

    val typedValue: T =
        stringValue.toTypedValue(defaultValue::class)
}
