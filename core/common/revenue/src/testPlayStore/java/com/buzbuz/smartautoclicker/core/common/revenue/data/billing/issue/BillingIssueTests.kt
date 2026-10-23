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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for [isNonFatal]. */
class BillingIssueTests {

    @Test
    fun `setup failure is never a non fatal`() {
        assertFalse(BillingIssue.SETUP_FAILED.isNonFatal(BillingResponseCode.SERVICE_DISCONNECTED))
        assertFalse(BillingIssue.SETUP_FAILED.isNonFatal(BillingResponseCode.DEVELOPER_ERROR))
    }

    @Test
    fun `query and acknowledge failures caused by the device state are not non fatals`() {
        listOf(BillingIssue.PURCHASES_QUERY_FAILED, BillingIssue.ACKNOWLEDGE_FAILED).forEach { issue ->
            assertFalse(issue.isNonFatal(BillingResponseCode.SERVICE_DISCONNECTED))
            assertFalse(issue.isNonFatal(BillingResponseCode.SERVICE_UNAVAILABLE))
            assertFalse(issue.isNonFatal(BillingResponseCode.BILLING_UNAVAILABLE))
            assertFalse(issue.isNonFatal(BillingResponseCode.ERROR))
            assertFalse(issue.isNonFatal(BillingResponseCode.NETWORK_ERROR))
        }
    }

    @Test
    fun `query and acknowledge failures caused by the app are non fatals`() {
        listOf(BillingIssue.PURCHASES_QUERY_FAILED, BillingIssue.ACKNOWLEDGE_FAILED).forEach { issue ->
            assertTrue(issue.isNonFatal(BillingResponseCode.DEVELOPER_ERROR))
            assertTrue(issue.isNonFatal(BillingResponseCode.ITEM_NOT_OWNED))
        }
    }

    @Test
    fun `purchase issues are always non fatals`() {
        assertTrue(BillingIssue.INVALID_SIGNATURE.isNonFatal(null))
        assertTrue(BillingIssue.CACHED_PURCHASE_MISSING.isNonFatal(null))
        assertTrue(BillingIssue.CACHED_PURCHASE_REVOKED.isNonFatal(null))
        assertTrue(BillingIssue.OWNED_PURCHASE_NOT_FOUND.isNonFatal(null))
        assertTrue(BillingIssue.UNKNOWN_PURCHASE_STATE.isNonFatal(null))
        assertTrue(BillingIssue.CONNECTION_START_FAILED.isNonFatal(null))
    }
}
