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
package com.buzbuz.gradle.obfuscation.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create


abstract class ObfuscationConfigPluginExtension {

    abstract val obfuscatedApplication: NamedDomainObjectContainer<ObfuscatedApplicationNamedObject>
    abstract val obfuscatedComponents: NamedDomainObjectContainer<ObfuscatedComponentNamedObject>

    internal lateinit var originalApplicationId: String
        private set
    internal lateinit var appNameRes: String
        private set
    internal var randomize: Boolean = false
        private set

    private var setupListener: (() -> Unit)? = null

    fun setup(applicationId: String, appNameResId: String, shouldRandomize: Boolean) {
        originalApplicationId = applicationId
        appNameRes = appNameResId
        randomize = shouldRandomize
        setupListener?.invoke()
    }


    internal companion object {
        private const val PLUGIN_EXTENSION_NAME = "obfuscationConfig"

        fun create(target: Project, onSetup: (() -> Unit)?) : ObfuscationConfigPluginExtension =
            target.extensions.create<ObfuscationConfigPluginExtension>(PLUGIN_EXTENSION_NAME).apply {
                setupListener = onSetup
            }
    }
}

