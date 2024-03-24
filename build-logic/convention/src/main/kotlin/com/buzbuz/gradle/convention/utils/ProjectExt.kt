/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.gradle.convention.utils

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.configure

internal inline fun Project.plugins(closure: PluginManager.() -> Unit) =
    closure(pluginManager)

internal inline fun Project.androidApp(crossinline closure: BaseAppModuleExtension.() -> Unit) =
    extensions.configure<BaseAppModuleExtension> { closure() }

internal inline fun Project.androidLib(crossinline closure: LibraryExtension.() -> Unit) =
    extensions.configure<LibraryExtension> { closure() }

internal inline fun Project.android(crossinline closure: BaseExtension.() -> Unit) =
    extensions.configure<BaseExtension> { closure() }
