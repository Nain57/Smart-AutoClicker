
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.libs.getLibs
import com.buzbuz.gradle.core.testImplementation
import com.buzbuz.gradle.core.android
import com.buzbuz.gradle.core.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidUnitTestConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.jetBrains.kotlin.android)
        }

        android {
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testOptions.unitTests.isIncludeAndroidResources = true
        }

        dependencies {
            testImplementation(libs.getLibrary("junit"))
            testImplementation(libs.getLibrary("androidx.test.core"))
            testImplementation(libs.getLibrary("androidx.test.ext.junit"))
            testImplementation(libs.getLibrary("mockito.core"))
            testImplementation(libs.getLibrary("mockito.kotlin"))
            testImplementation(libs.getLibrary("mockk.android"))
            testImplementation(libs.getLibrary("robolectric"))
        }
    }
}