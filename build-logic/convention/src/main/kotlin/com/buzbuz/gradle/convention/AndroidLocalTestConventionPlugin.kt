
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.libs.getLibs
import com.buzbuz.gradle.core.android
import com.buzbuz.gradle.core.androidTestImplementation
import com.buzbuz.gradle.core.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidLocalTestConventionPlugin : Plugin<Project> {

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
            androidTestImplementation(libs.getLibrary("junit"))
            androidTestImplementation(libs.getLibrary("androidx.test.core"))
            androidTestImplementation(libs.getLibrary("androidx.test.ext.junit"))
            androidTestImplementation(libs.getLibrary("androidx.test.runner"))
            androidTestImplementation(libs.getLibrary("androidx.test.rules"))
        }
    }
}