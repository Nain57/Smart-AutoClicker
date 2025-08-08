
package com.buzbuz.gradle.parameters

import com.buzbuz.gradle.core.isBuildForVariant
import org.gradle.api.Project
import org.gradle.api.provider.Property

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
        properties[parameterName]?.let { return BuildParameter(rootProject, parameterName, it as String) }

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

        return BuildParameter(rootProject, parameterName, parameterValue as? String)
    }

    fun isBuildForVariant(variantName: String): Boolean =
        rootProject.isBuildForVariant(variantName)

    private companion object {
        const val LOCAL_PROPERTIES_FILE = "local.properties"
    }
}
