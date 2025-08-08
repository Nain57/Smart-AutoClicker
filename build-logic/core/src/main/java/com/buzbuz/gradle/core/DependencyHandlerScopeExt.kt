
package com.buzbuz.gradle.core

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.implementation(dependency: Provider<MinimalExternalModuleDependency>) =
    add("implementation", dependency)

fun DependencyHandlerScope.playStoreImplementation(dependency: Provider<MinimalExternalModuleDependency>) =
    add("playStoreImplementation", dependency)

fun DependencyHandlerScope.ksp(dependency: Provider<MinimalExternalModuleDependency>) =
    add("ksp", dependency)

fun DependencyHandlerScope.kspTest(dependency: Provider<MinimalExternalModuleDependency>) =
    add("kspTest", dependency)

fun DependencyHandlerScope.testImplementation(dependency: Provider<MinimalExternalModuleDependency>) =
    add("testImplementation", dependency)

fun DependencyHandlerScope.androidTestImplementation(dependency: Provider<MinimalExternalModuleDependency>) =
    add("androidTestImplementation", dependency)