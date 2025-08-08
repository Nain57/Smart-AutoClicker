
package com.buzbuz.gradle.parameters

import com.android.build.api.dsl.LibraryProductFlavor
import com.buzbuz.gradle.core.isBuildForVariant
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

