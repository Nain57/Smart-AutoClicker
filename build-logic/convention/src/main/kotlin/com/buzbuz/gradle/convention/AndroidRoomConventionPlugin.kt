
package com.buzbuz.gradle.convention

import androidx.room.gradle.RoomExtension

import com.buzbuz.gradle.core.libs.getLibs
import com.buzbuz.gradle.core.implementation
import com.buzbuz.gradle.core.ksp
import com.buzbuz.gradle.core.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.androidX.room)
            apply(libs.plugins.google.ksp)
        }

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        dependencies {
            implementation(libs.getLibrary("androidx.room.runtime"))
            implementation(libs.getLibrary("androidx.room.ktx"))
            ksp(libs.getLibrary("androidx.room.compiler"))
        }
    }
}