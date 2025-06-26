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
package com.buzbuz.gradle.parameters

import com.buzbuz.gradle.core.extensions.isBuildForVariant
import com.android.build.api.dsl.LibraryProductFlavor
import org.gradle.api.GradleException


fun LibraryProductFlavor.manifestPlaceholders(parameter: BuildParameter<*>) {
    if (!parameter.rootProject.isBuildForVariant(name)) return
    manifestPlaceholders[parameter.name] = parameter.stringValue
}

fun LibraryProductFlavor.buildConfigField(parameter: BuildParameter<*>) {
    if (!parameter.rootProject.isBuildForVariant(name)) return

    when (parameter.typedValue) {
        is Int -> intBuildConfigField(parameter)
        is String -> stringBuildConfigField(parameter)
        is Array<*> -> stringArrayBuildConfigField(parameter)
        else -> throw GradleException("ERROR: Unsupported type as build config field ${parameter.name}")
    }
}

private fun LibraryProductFlavor.intBuildConfigField(parameter: BuildParameter<*>) {
    buildConfigField(
        type = "int",
        name = parameter.name.asBuildConfigFieldName(),
        value = parameter.stringValue,
    )
}

private fun LibraryProductFlavor.stringBuildConfigField(parameter: BuildParameter<*>) {
    buildConfigField(
        type = "String",
        name = parameter.name.asBuildConfigFieldName(),
        value = "\"${parameter.stringValue}\"",
    )
}

private fun LibraryProductFlavor.stringArrayBuildConfigField(parameter: BuildParameter<*>) {
    buildConfigField(
        type = "String[]",
        name = parameter.name.asBuildConfigFieldName(),
        value = parameter.stringValue,
    )
}

private fun String.asBuildConfigFieldName(): String =
    buildString {
        this@asBuildConfigFieldName.forEach { char ->
            if (char.isUpperCase()) append('_').append(char)
            else append(char.uppercaseChar())
        }
    }