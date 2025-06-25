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

import com.buzbuz.gradle.core.model.KlickrDimension
import com.buzbuz.gradle.core.model.KlickrFlavour
import com.buzbuz.gradle.core.extensions.android
import org.gradle.api.Plugin
import org.gradle.api.Project

class FlavourConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        android {
            flavorDimensions(*KlickrDimension.values().map { it.flavourDimensionName }.toTypedArray())

            productFlavors {
                KlickrFlavour.values().forEach { flavour ->
                    create(flavour.flavourName) {
                        dimension = flavour.dimension.flavourDimensionName
                    }
                }
            }
        }
    }
}


