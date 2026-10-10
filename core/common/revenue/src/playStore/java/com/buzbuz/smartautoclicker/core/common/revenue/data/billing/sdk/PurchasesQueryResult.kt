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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk

import com.android.billingclient.api.Purchase

internal sealed interface PurchasesQueryResult {

    /** Google Play answered the query. A null [purchase] means the product is not owned. */
    data class Success(val purchase: Purchase?) : PurchasesQueryResult

    /** The query couldn't be completed, the ownership of the product is unknown. */
    data class Failure(val responseCode: Int, val debugMessage: String) : PurchasesQueryResult
}
