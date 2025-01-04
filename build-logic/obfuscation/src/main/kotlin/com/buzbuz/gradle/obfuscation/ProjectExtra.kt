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
package com.buzbuz.gradle.obfuscation

import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra


internal fun Project.sourceFolderPath(): String =
    layout.projectDirectory.toString()


internal fun Project.setExtraOriginalApplicationId(appId: String): Unit =
    rootProject.extra.set(EXTRA_APPLICATION_ID, appId)
fun Project.getExtraOriginalApplicationId(): String =
    rootProject.extra.get(EXTRA_APPLICATION_ID) as String

internal fun Project.setExtraActualApplicationId(appId: String): Unit =
    rootProject.extra.set(EXTRA_ACTUAL_APPLICATION_ID, appId)
fun Project.getExtraActualApplicationId(): String =
    rootProject.extra.get(EXTRA_ACTUAL_APPLICATION_ID) as String


private const val EXTRA_APPLICATION_ID = "obfuscation_applicationId"
private const val EXTRA_ACTUAL_APPLICATION_ID = "obfuscation_actual_applicationId"
