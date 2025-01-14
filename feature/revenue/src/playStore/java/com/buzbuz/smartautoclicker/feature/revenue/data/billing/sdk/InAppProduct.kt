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
@file:Suppress("DEPRECATION")

package com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails


internal sealed class InAppProduct {

    abstract val productDetails: Any

    abstract val title: String
    abstract val description: String
    abstract val price: String

    data class Modern(override val productDetails: ProductDetails): InAppProduct() {
        override val title: String = productDetails.title
        override val description = productDetails.description
        override val price: String= productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
    }

    data class Legacy(override val productDetails: SkuDetails): InAppProduct() {
        override val title: String = productDetails.title
        override val description = productDetails.description
        override val price: String= productDetails.price
    }

    data class Debug(
        override val productDetails: Any = Unit,
        override val title: String = "Test Product",
        override val description: String = "Test description",
        override val price: String = "4.99€",
    ) : InAppProduct()
}

internal fun ProductDetails.toInAppProduct(): InAppProduct =
    InAppProduct.Modern(this)

internal fun SkuDetails.toInAppProduct(): InAppProduct =
    InAppProduct.Legacy(this)
