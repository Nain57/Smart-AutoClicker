
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.libs.getLibs
import com.buzbuz.gradle.core.playStoreImplementation
import com.buzbuz.gradle.core.android
import com.buzbuz.gradle.core.plugins

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class CrashlyticsConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.google.crashlytics)
            apply(libs.plugins.google.gms)
        }

        android {
            buildTypes {
                getByName("release") {
                    configure<CrashlyticsExtension> {
                        nativeSymbolUploadEnabled = true
                    }
                }
            }
        }

        dependencies {
            playStoreImplementation(platform(libs.getLibrary("google.firebase.bom")))
            playStoreImplementation(libs.getLibrary("google.firebase.crashlytics.ktx"))
            playStoreImplementation(libs.getLibrary("google.firebase.crashlytics.ndk"))
        }
    }
}