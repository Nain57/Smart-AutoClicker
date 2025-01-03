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
import com.buzbuz.gradle.core.android
import com.buzbuz.gradle.obfuscation.component.ConfigFileContentBuilder
import com.buzbuz.gradle.obfuscation.component.ObfuscatedComponent
import com.buzbuz.gradle.obfuscation.component.toDomain
import com.buzbuz.gradle.obfuscation.extensions.ObfuscationConfigPluginExtension
import com.buzbuz.gradle.obfuscation.tasks.registerCleanupTask
import com.buzbuz.gradle.obfuscation.tasks.registerRandomizeTask

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import kotlin.random.Random

class ObfuscationPlugin : Plugin<Project> {

    private val random: Random = Random(System.currentTimeMillis())

    private lateinit var target: Project

    private lateinit var configPluginExtension: ObfuscationConfigPluginExtension
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
            shouldRandomize = configPluginExtension.randomize,
        )
    }

    private fun afterEvaluate() {
        // Create values for the manifests
        registerManifestPlaceholders(
            target = target,
            components = obfuscatedComponents,
            shouldRandomize = configPluginExtension.randomize,
        )

        if (!configPluginExtension.randomize) return

        obfuscatedComponents.forEach { component ->
            target.logger.info("Randomizing ${component.originalClassName}")

            val randomizeTask = target.registerRandomizeTask(component)
            val cleanupTask = target.registerCleanupTask(component)

            // Hook the tasks into the build lifecycle
            target.tasks.withType(KotlinCompile::class.java).configureEach {
                dependsOn(randomizeTask)
                finalizedBy(cleanupTask)
            }
        }
    }

    private fun generateApplicationId(shouldRandomize: Boolean, regularApplicationId: String): String =
        if (shouldRandomize) random.nextApplicationId()
        else regularApplicationId

    private fun createComponentConfigFile(target: Project, originalAppId: String, components: List<ObfuscatedComponent>, shouldRandomize: Boolean) {
        val configFileContent = ConfigFileContentBuilder().apply {
            appId = originalAppId
            obfuscatedComponents  = components
            isRandomized = shouldRandomize
        }.build()

        val path = "${target.layout.projectDirectory}/src/main/java/${originalAppId.replace('.', '/')}"
        val configFile = File(path, COMPONENT_CONFIG_FILE_NAME)

        target.logger.info("Create component config file at $path")

        configFile.writeText(configFileContent)
    }

    private fun registerManifestPlaceholders(target: Project, components: List<ObfuscatedComponent>, shouldRandomize: Boolean) {
        target.extensions.findByType(AppExtension::class.java)?.apply {
            this.applicationVariants.forEach { variant ->
                components.forEach { component ->
                    variant.mergedFlavor.manifestPlaceholders[component.manifestPlaceholderKey] =
                        if (shouldRandomize) component.manifestPlaceholderValue
                        else component.originalComponentName
                }
            }
        }
    }

    private companion object {
        private const val COMPONENT_CONFIG_FILE_NAME = "ComponentConfig.kt"
    }
}
