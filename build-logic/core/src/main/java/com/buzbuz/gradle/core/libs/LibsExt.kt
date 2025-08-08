

package com.buzbuz.gradle.core.libs

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType


fun Project.getLibs(): VersionCatalogWrapper =
    VersionCatalogWrapper(extensions.getByType<VersionCatalogsExtension>().named("libs"))


class VersionCatalogWrapper internal constructor(private val libs: VersionCatalog) {

    val plugins: Plugins by lazy { Plugins(libs) }
    val versions: Versions by lazy { Versions(libs) }

    fun getLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
        libs.findLibrary(alias).get()
}











