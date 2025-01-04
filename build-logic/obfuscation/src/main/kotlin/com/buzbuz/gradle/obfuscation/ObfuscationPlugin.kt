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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.buzbuz.gradle.obfuscation.application.ObfuscatedApplication
import com.buzbuz.gradle.obfuscation.application.toDomain
import com.buzbuz.gradle.obfuscation.component.ConfigFileContentBuilder
import com.buzbuz.gradle.obfuscation.component.ObfuscatedComponent
import com.buzbuz.gradle.obfuscation.component.toDomain
import com.buzbuz.gradle.obfuscation.extensions.ObfuscationConfigPluginExtension
import com.buzbuz.gradle.obfuscation.tasks.registerCleanupTask
import com.buzbuz.gradle.obfuscation.tasks.registerRandomizeApplicationTask
import com.buzbuz.gradle.obfuscation.tasks.registerRandomizeComponentTask

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import kotlin.random.Random

class ObfuscationPlugin : Plugin<Project> {

    private val random: Random = Random(System.currentTimeMillis())

    private lateinit var target: Project

    private lateinit var configPluginExtension: ObfuscationConfigPluginExtension
    private val obfuscatedApplication: ObfuscatedApplication by lazy {
        configPluginExtension.obfuscatedApplication.first().toDomain(target, random)
    }
    private val obfuscatedComponents: List<ObfuscatedComponent> by lazy {
        configPluginExtension.obfuscatedComponents.map { namedObject ->
            namedObject.toDomain(target, random)
        }
    }


    override fun apply(target: Project) {
        this.target = target
        configPluginExtension = ObfuscationConfigPluginExtension.create(
            target = target,
            onSetup = ::onConfigure,
        )

        target.afterEvaluate {
            afterEvaluate()
        }
    }

    private fun onConfigure() {
        // Delete the build folder if we randomize
        if (configPluginExtension.randomize) {
            target.layout.buildDirectory.asFileTree.files.forEach { buildFile ->
                buildFile.delete()
            }
        }

        // Generate the randomized application id, if needed
        val actualAppId = generateApplicationId(
            shouldRandomize = configPluginExtension.randomize,
            regularApplicationId = configPluginExtension.originalApplicationId,
        )
        target.setExtraOriginalApplicationId(configPluginExtension.originalApplicationId)
        target.setExtraActualApplicationId(actualAppId)

        // Get the component configuration file for references in the code
        createComponentConfigFile(
            target = target,
            originalAppId = configPluginExtension.originalApplicationId,
            components = obfuscatedComponents,
        )
    }

    private fun afterEvaluate() {
        // Create values for the manifests
        registerManifestPlaceholders(target)

        if (!configPluginExtension.randomize) return

        val randomizeApplicationTask = target.registerRandomizeApplicationTask(obfuscatedApplication)
        val cleanupApplicationTask = target.registerCleanupTask(obfuscatedApplication)

        target.tasks.withType(KotlinCompile::class.java).configureEach {
            finalizedBy(cleanupApplicationTask)
        }

        obfuscatedComponents.forEach { component ->
            target.logger.info("Randomizing ${component.originalClassName}")

            // Hook the task into the build lifecycle
            val randomizeComponentsTask = target.registerRandomizeComponentTask(component)
            randomizeComponentsTask.dependsOn(randomizeApplicationTask)
            target.tasks.withType(KotlinCompile::class.java).configureEach {
                dependsOn(randomizeComponentsTask)
            }

            // Hook cleanupComponentsTask to run after the `mergeProjectDex` task
            val cleanupComponentsTask = target.registerCleanupTask(component)
            target.tasks.first { task -> task.name.startsWith("package") }
                .finalizedBy(cleanupComponentsTask)
        }
    }

    private fun generateApplicationId(shouldRandomize: Boolean, regularApplicationId: String): String =
        if (shouldRandomize) random.nextApplicationId()
        else regularApplicationId

    private fun createComponentConfigFile(target: Project, originalAppId: String, components: List<ObfuscatedComponent>) {
        val configFileContent = ConfigFileContentBuilder().apply {
            appId = originalAppId
            obfuscatedComponents  = components
            isRandomized = configPluginExtension.randomize
        }.build()

        val path = "${target.layout.projectDirectory}/src/main/java/${originalAppId.replace('.', '/')}"
        val configFile = File(path, COMPONENT_CONFIG_FILE_NAME)

        target.logger.info("Create component config file at $path")

        configFile.writeText(configFileContent)
    }

    private fun registerManifestPlaceholders(target: Project) {
        target.extensions.findByType(AppExtension::class.java)?.apply {
            this.applicationVariants.forEach { variant ->
                // Register the app name
                variant.mergedFlavor.manifestPlaceholders[MANIFEST_PLACEHOLDER_APP_NAME] =
                    if (configPluginExtension.randomize) random.nextString(10)
                    else configPluginExtension.appNameRes

                // Register the application
                variant.mergedFlavor.manifestPlaceholders[obfuscatedApplication.manifestPlaceholderKey] =
                    if (configPluginExtension.randomize) obfuscatedApplication.manifestPlaceholderValue
                    else obfuscatedApplication.originalFullClassName

                // Register the components
                obfuscatedComponents.forEach { component ->
                    variant.mergedFlavor.manifestPlaceholders[component.manifestPlaceholderKey] =
                        if (configPluginExtension.randomize) component.manifestPlaceholderValue
                        else component.originalComponentName
                }
            }
        }
    }

    private companion object {
        private const val MANIFEST_PLACEHOLDER_APP_NAME = "appName"
        private const val COMPONENT_CONFIG_FILE_NAME = "ComponentConfig.kt"
    }
}
