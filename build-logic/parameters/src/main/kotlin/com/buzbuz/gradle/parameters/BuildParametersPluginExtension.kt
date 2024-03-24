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
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

import java.io.FileInputStream
import java.util.Properties

abstract class BuildParametersPluginExtension {

    abstract val project: Property<Project>

    @Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS", "KotlinRedundantDiagnosticSuppress")
    private val rootProject: Project
        get() = project.get()?.rootProject
            ?: throw IllegalStateException("Accessing project before plugin apply")

    private val properties: MutableMap<String, Any> = mutableMapOf()

    operator fun get(parameterName: String): BuildParameter {
        properties[parameterName]?.let { return BuildParameter(parameterName, it as String) }

        val parameterValue =
            if (rootProject.hasProperty(parameterName)) rootProject.properties[parameterName]
            else {
                rootProject.file(LOCAL_PROPERTIES_FILE).let { localPropertiesFile ->
                    if (localPropertiesFile.exists()) {
                        val localProperties = Properties().apply { load(FileInputStream(localPropertiesFile)) }
                        localProperties[parameterName]
                    } else null
                }
            }

        if (parameterValue == null) {
            rootProject.logger.warn("WARNING: Build property $parameterName was not found")
            return BuildParameter(parameterName, null)
        }

        return BuildParameter(parameterName, parameterValue as String)
    }

    fun isBuildForVariant(variantName: String): Boolean =
        rootProject.let { project ->
            val normalizedName = variantName.uppercaseFirstChar()

            return project.gradle.startParameter.taskRequests.find { taskExecRequest ->
                taskExecRequest.args.find { taskName -> taskName.contains(normalizedName) } != null
            } != null
        }

    private companion object {
        const val LOCAL_PROPERTIES_FILE = "local.properties"
    }
}
