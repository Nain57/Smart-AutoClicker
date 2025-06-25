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
package com.buzbuz.gradle.core.extensions

import com.android.build.api.dsl.ProductFlavor
import com.buzbuz.gradle.core.model.KlickrBuildType
import com.buzbuz.gradle.core.model.KlickrFlavour
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

/**
 * For fDroid flavour configuration in build.gradle.kts
 * Usage: android.productFlavors.fDroid { ... }
 */
fun <T : ProductFlavor> NamedDomainObjectContainer<T>.fDroid(configureAction: T.() -> Unit) {
    getProductFlavour(KlickrFlavour.F_DROID, configureAction)
}

/**
 * For playStore flavour configuration in build.gradle.kts
 * Usage: android.productFlavors.playStore { ... }
 */
fun <T : ProductFlavor> NamedDomainObjectContainer<T>.playStore(configureAction: T.() -> Unit) {
    getProductFlavour(KlickrFlavour.PLAY_STORE, configureAction)
}

internal fun getVariantName(flavour: KlickrFlavour?, buildType: KlickrBuildType?): String? =
    when {
        flavour != null && buildType != null ->
            "${flavour.flavourName}${buildType.buildTypeName.uppercaseFirstChar()}"
        flavour != null && buildType == null ->
            flavour.flavourName
        flavour == null && buildType != null ->
            buildType.buildTypeName
        else ->
            null
    }

private fun <T : ProductFlavor> NamedDomainObjectContainer<T>.getProductFlavour(
    flavour: KlickrFlavour,
    configureAction: T.() -> Unit,
) = getByName(flavour.flavourName, configureAction)