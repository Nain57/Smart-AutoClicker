
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

