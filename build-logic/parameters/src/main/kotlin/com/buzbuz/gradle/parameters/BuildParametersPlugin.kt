
package com.buzbuz.gradle.parameters

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create


class BuildParametersPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        extensions.create<BuildParametersPluginExtension>(PLUGIN_EXTENSION_NAME).apply {
            project.set(target)
        }
    }

    private companion object {
        const val PLUGIN_EXTENSION_NAME = "buildParameters"
    }
}
