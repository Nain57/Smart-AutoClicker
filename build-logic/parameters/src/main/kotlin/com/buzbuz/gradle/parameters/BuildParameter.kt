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
import org.gradle.api.GradleException
import org.gradle.api.Project


class BuildParameter(private val project: Project, private val name: String, private val value: String?) {

    fun asString(): String? {
        if (value == null) project.logger.info("INFO: Build property $name not found.")
        return value
    }

    fun asBoolean(): Boolean {
        return value == "true" || value == "TRUE"
    }

    fun asIntBuildConfigField(variant: LibraryProductFlavor, default: Int? = null) {
        if (!project.isBuildForVariant(variant.name)) return

        if (value == null && default == null)
            throw GradleException("ERROR: Build property $name not found, cannot set BuildConfig field.")

        variant.buildConfigField(
            type = "int",
            name = name.asBuildConfigFieldName(),
            value = value ?: default.toString(),
        )
    }

    fun asStringBuildConfigField(variant: LibraryProductFlavor, default: String? = null) {
        if (!project.isBuildForVariant(variant.name)) return

        if (value == null && default == null)
            throw GradleException("ERROR: Build property $name not found, cannot set BuildConfig field.")

        val fieldValue = value ?: default!!
        variant.buildConfigField(
            type = "String",
            name = name.asBuildConfigFieldName(),
            value = "\"$fieldValue\"",
        )
    }

    fun asStringArrayBuildConfigField(variant: LibraryProductFlavor) {
        if (!project.isBuildForVariant(variant.name)) return

        variant.buildConfigField(
            type = "String[]",
            name = name.asBuildConfigFieldName(),
            value = value ?: "{}",
        )
    }

    fun asManifestPlaceHolder(variant: LibraryProductFlavor, default: String? = null) {
        if (!project.isBuildForVariant(variant.name)) return

        if (value == null && default == null)
            throw GradleException("ERROR: Build property $name not found, cannot set manifest placeholder.")

        variant.manifestPlaceholders[name] = value ?: default!!
    }

    private fun String.asBuildConfigFieldName(): String =
        buildString {
            this@asBuildConfigFieldName.forEach { char ->
                if (char.isUpperCase()) append('_').append(char)
                else append(char.uppercaseChar())
            }
        }
}

