
package com.buzbuz.gradle.core.libs

import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalog

class Versions internal constructor(private val libs: VersionCatalog) {

    val android: Android by lazy { Android() }

    inner class Android internal constructor() {
        val compileSdk: Int
            get() = libs.getVersion("androidCompileSdk")
        val minSdk: Int
            get() = libs.getVersion("androidMinSdk")
    }

    val java: JavaVersion
        get() = JavaVersion.valueOf("VERSION_${libs.getVersion("java")}")

    val jvmTarget: String
        get() = libs.getStringVersion("java")


    private fun VersionCatalog.getVersion(alias: String): Int =
        getStringVersion(alias).toInt()

    private fun VersionCatalog.getStringVersion(alias: String): String =
        findVersion(alias).get().requiredVersion
}