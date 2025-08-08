
package com.buzbuz.gradle.core.libs

import org.gradle.api.artifacts.VersionCatalog


class Plugins internal constructor(private val libs: VersionCatalog) {

    val android: Android by lazy { Android() }
    val androidX: AndroidX by lazy { AndroidX() }
    val jetBrains: JetBrains by lazy { JetBrains() }
    val google: Google by lazy { Google() }

    inner class Android internal constructor() {
        val application: String
            get() = libs.getPluginId("androidApplication")
        val library: String
            get() = libs.getPluginId("androidLibrary")
    }

    inner class AndroidX internal constructor() {
        val room: String
            get() = libs.getPluginId("androidxRoom")
    }

    inner class JetBrains internal constructor() {

        val kotlin: Kotlin by lazy { Kotlin() }

        inner class Kotlin {
            val android: String
                get() = libs.getPluginId("jetbrainsKotlinAndroid")
            val serialization: String
                get() = libs.getPluginId("jetbrainsKotlinSerialization")
        }
    }

    inner class Google internal constructor() {
        val ksp: String
            get() = libs.getPluginId("googleKsp")
        val daggerHiltAndroid: String
            get() = libs.getPluginId("googleDaggerHiltAndroid")
        val crashlytics: String
            get() = libs.getPluginId("googleCrashlytics")
        val gms: String
            get() = libs.getPluginId("googleGms")
    }

    private fun VersionCatalog.getPluginId(alias: String): String =
        findPlugin(alias).get().get().pluginId
}