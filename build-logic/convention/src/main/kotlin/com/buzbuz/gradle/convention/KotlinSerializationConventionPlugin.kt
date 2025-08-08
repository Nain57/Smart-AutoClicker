
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.libs.getLibs
import com.buzbuz.gradle.core.implementation
import com.buzbuz.gradle.core.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KotlinSerializationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.jetBrains.kotlin.serialization)
        }

        dependencies {
            implementation(libs.getLibrary("kotlinx.serialization.json"))
        }
    }
}