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
package com.buzbuz.gradle.parameters

import com.android.build.api.dsl.LibraryProductFlavor

data class BuildParameter(val name: String, val value: String?) {

    fun asStringBuildConfigField(flavor: LibraryProductFlavor) {
        flavor.buildConfigField(
            type = "String",
            name = name.asBuildConfigFieldName(),
            value = "\"$value\""
        )
    }

    fun asStringArrayBuildConfigField(flavor: LibraryProductFlavor) {
        flavor.buildConfigField(
            type = "String[]",
            name = name.asBuildConfigFieldName(),
            value = value ?: "",
        )
    }

    fun asManifestPlaceHolder(flavor: LibraryProductFlavor) {
        flavor.manifestPlaceholders[name] = value ?: ""
    }

    private fun String.asBuildConfigFieldName(): String =
        buildString {
            this@asBuildConfigFieldName.forEach { char ->
                if (char.isUpperCase()) append('_').append(char)
                else append(char.uppercaseChar())
            }
        }
}

