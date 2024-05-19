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

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams


internal fun legacyInAppProductDetailsQueryParams(productId: String) =
    SkuDetailsParams.newBuilder()
        .setSkusList(listOf(productId))
        .setType(BillingClient.SkuType.INAPP)
        .build()

internal fun legacyBillingFlowQueryParams(sku: SkuDetails) =
    BillingFlowParams.newBuilder()
        .setSkuDetails(sku)
        .build()

internal fun List<SkuDetails>.findLegacyProduct(productId: String): InAppProduct? =
    find { it.sku == productId }?.toInAppProduct()