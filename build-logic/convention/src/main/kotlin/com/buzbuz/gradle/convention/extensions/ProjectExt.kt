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
package com.buzbuz.gradle.convention.extensions

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.buzbuz.gradle.convention.libs.VersionCatalogWrapper
import com.buzbuz.gradle.convention.model.KlickrBuildType
import com.buzbuz.gradle.convention.model.KlickrFlavour
import com.google.protobuf.gradle.ProtobufExtension

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

/**
 * Check if the current build is for the provided variant.
 * This method will check if the variant name is contained in the command that executed the build.
 */
fun Project.isBuildForVariant(variantName: String?): Boolean {
    if (variantName == null) return false
    val normalizedName = variantName.uppercaseFirstChar()

    return project.gradle.startParameter.taskRequests.find { taskExecRequest ->
        taskExecRequest.args.find { taskName -> taskName.contains(normalizedName) } != null
    } != null
}

fun Project.isBuildForVariant(flavour: KlickrFlavour? = null, buildType: KlickrBuildType? = null): Boolean =
    isBuildForVariant(getVariantName(flavour, buildType))


internal fun Project.getLibs(): VersionCatalogWrapper =
    VersionCatalogWrapper(extensions.getByType<VersionCatalogsExtension>().named("libs"))

internal inline fun Project.plugins(closure: PluginManager.() -> Unit) =
    closure(pluginManager)

internal inline fun Project.androidApp(crossinline closure: BaseAppModuleExtension.() -> Unit) =
    extensions.configure<BaseAppModuleExtension> { closure() }

internal inline fun Project.androidLib(crossinline closure: LibraryExtension.() -> Unit) =
    extensions.configure<LibraryExtension> { closure() }

internal inline fun Project.android(crossinline closure: BaseExtension.() -> Unit) =
    extensions.configure<BaseExtension> { closure() }

internal inline fun Project.protobuf(crossinline closure: ProtobufExtension.() -> Unit) =
    extensions.configure<ProtobufExtension> { closure() }