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
