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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue

import com.android.billingclient.api.BillingClient.BillingResponseCode

internal enum class BillingIssue {
    /** Expected with auto service reconnection, the next API call reconnects. Never sent as a non fatal. */
    SETUP_FAILED,
    CONNECTION_START_FAILED,
    PURCHASES_QUERY_FAILED,
    ACKNOWLEDGE_FAILED,
    INVALID_SIGNATURE,
    UNKNOWN_PURCHASE_STATE,
    /** Sent once when the grace period starts, not for each query during it. */
    CACHED_PURCHASE_MISSING,
    CACHED_PURCHASE_REVOKED,
    /** The billing flow reported the product as owned, but the purchases query never returned it. */
    OWNED_PURCHASE_NOT_FOUND,
}

/** True if the issue is worth a non fatal, false if it is only kept as context for other reports. */
internal fun BillingIssue.isNonFatal(responseCode: Int?): Boolean =
    when (this) {
        BillingIssue.SETUP_FAILED -> false
        BillingIssue.PURCHASES_QUERY_FAILED,
        BillingIssue.ACKNOWLEDGE_FAILED -> responseCode !in DEVICE_STATE_RESPONSE_CODES
        else -> true
    }

/**
 * Codes caused by the state of the device, Play or the network. Failed queries are retried, and there is nothing to fix
 * in the app for them.
 */
private val DEVICE_STATE_RESPONSE_CODES = setOf(
    BillingResponseCode.SERVICE_DISCONNECTED,
    BillingResponseCode.SERVICE_UNAVAILABLE,
    BillingResponseCode.BILLING_UNAVAILABLE,
    BillingResponseCode.ERROR,
    BillingResponseCode.NETWORK_ERROR,
)
