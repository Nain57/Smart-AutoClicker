
package com.buzbuz.gradle.convention

import com.android.build.api.dsl.ProductFlavor
import com.buzbuz.gradle.core.android
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project

class FlavourConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        android {
            flavorDimensions(KLICKR_FLAVOUR_DIMENSION_VERSION)

            productFlavors {
                create(KLICKR_VERSION_FLAVOUR_F_DROID) {
                    dimension = KLICKR_FLAVOUR_DIMENSION_VERSION
                }
                create(KLICKR_VERSION_FLAVOUR_PLAY_STORE) {
                    dimension = KLICKR_FLAVOUR_DIMENSION_VERSION
                }
            }
        }
    }
}


fun <T : ProductFlavor> NamedDomainObjectContainer<T>.fDroid(configureAction: T.() -> Unit) {
    getByName(KLICKR_VERSION_FLAVOUR_F_DROID, configureAction)
}

fun <T : ProductFlavor> NamedDomainObjectContainer<T>.playStore(configureAction: T.() -> Unit) {
    getByName(KLICKR_VERSION_FLAVOUR_PLAY_STORE, configureAction)
}

const val KLICKR_FLAVOUR_DIMENSION_VERSION = "version"
const val KLICKR_VERSION_FLAVOUR_F_DROID = "fDroid"
const val KLICKR_VERSION_FLAVOUR_PLAY_STORE = "playStore"
