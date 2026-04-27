/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.gradle.convention.plugins

import com.buzbuz.gradle.convention.extensions.androidApp
import com.buzbuz.gradle.convention.extensions.androidLib
import com.buzbuz.gradle.convention.model.KlickrDimension
import com.buzbuz.gradle.convention.model.KlickrFlavour
import org.gradle.api.Plugin
import org.gradle.api.Project

class FlavourConventionPlugin : Plugin<Project> {

    // I have tried to generify this for hours, but it doesn't seem to be possible.
    // So do forget to mirror your changes in both blocks
    override fun apply(target: Project): Unit = with(target) {
        androidApp {
            flavorDimensions.clear()
            flavorDimensions.addAll(KlickrDimension.entries.map { it.flavourDimensionName })


            productFlavors {
                KlickrFlavour.entries.forEach { flavour ->
                    create(flavour.flavourName) {
                        dimension = flavour.dimension.flavourDimensionName
                    }
                }
            }
        }

        androidLib {
            flavorDimensions.clear()
            flavorDimensions.addAll(KlickrDimension.entries.map { it.flavourDimensionName })

            productFlavors {
                KlickrFlavour.entries.forEach { flavour ->
                    create(flavour.flavourName) {
                        dimension = flavour.dimension.flavourDimensionName
                    }
                }
            }
        }
    }
}
