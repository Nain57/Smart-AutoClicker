
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.libs.getLibs
import com.buzbuz.gradle.core.implementation
import com.buzbuz.gradle.core.ksp
import com.buzbuz.gradle.core.kspTest
import com.buzbuz.gradle.core.testImplementation
import com.buzbuz.gradle.core.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.google.ksp)
            apply(libs.plugins.google.daggerHiltAndroid)
        }

        dependencies {
            implementation(libs.getLibrary("google.dagger.hilt"))
            ksp(libs.getLibrary("google.dagger.hilt.compiler"))

            testImplementation(libs.getLibrary("google.dagger.hilt.testing"))
            kspTest(libs.getLibrary("google.dagger.hilt.compiler"))
        }
    }
}