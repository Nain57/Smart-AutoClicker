
package com.buzbuz.gradle.core

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

/**
 * Check if the current build is for the provided variant.
 * This method will check if the variant name is contained in the command that executed the build.
 */
fun Project.isBuildForVariant(variantName: String): Boolean {
    val normalizedName = variantName.uppercaseFirstChar()

    return project.gradle.startParameter.taskRequests.find { taskExecRequest ->
        taskExecRequest.args.find { taskName -> taskName.contains(normalizedName) } != null
    } != null
}


inline fun Project.plugins(closure: PluginManager.() -> Unit) =
    closure(pluginManager)

inline fun Project.androidApp(crossinline closure: BaseAppModuleExtension.() -> Unit) =
    extensions.configure<BaseAppModuleExtension> { closure() }

inline fun Project.androidLib(crossinline closure: LibraryExtension.() -> Unit) =
    extensions.configure<LibraryExtension> { closure() }

inline fun Project.android(crossinline closure: BaseExtension.() -> Unit) =
    extensions.configure<BaseExtension> { closure() }
