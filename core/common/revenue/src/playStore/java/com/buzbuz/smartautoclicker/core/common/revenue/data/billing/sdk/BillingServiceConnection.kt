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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk

import android.content.Context
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssue
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssueReporter
import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject

/**
 * Connects the billing client once. The client has auto service reconnection enabled, so every later API call
 * re-establishes the connection by itself if it was lost or if this initial setup failed.
 */
internal class BillingServiceConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val issueReporter: BillingIssueReporter,
) {

    private var clientProxy: BillingClientProxy? = null

    fun connect(
        productId: String,
        onClientAvailable: (BillingClientProxy) -> Unit,
        onNewPurchasesListener: (Purchase?) -> Unit,
    ) {
        if (clientProxy != null) return

        val proxy = BillingClientProxy(context, productId, onNewPurchasesListener)
        clientProxy = proxy

        try {
            proxy.client.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected(): Unit = onDisconnected()
                override fun onBillingSetupFinished(p0: BillingResult): Unit = onSetupResult(p0, proxy, onClientAvailable)
            })
        } catch (isex: IllegalStateException) {
            Log.e(TAG, "Can't start billing service connection", isex)
            onClientAvailable(proxy)
        }
    }

    private fun onSetupResult(
        billingResult: BillingResult,
        proxy: BillingClientProxy,
        onClientAvailable: (BillingClientProxy) -> Unit,
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        Log.d(TAG, "onBillingSetupFinished: $responseCode $debugMessage")

        if (responseCode != BillingClient.BillingResponseCode.OK) {
            issueReporter.report(BillingIssue.SETUP_FAILED, responseCode, debugMessage)
        }

        // Even on failure, the client will try to reconnect on the next API call.
        onClientAvailable(proxy)
    }

    private fun onDisconnected() {
        Log.i(TAG, "onBillingServiceDisconnected, will reconnect on next billing call")
    }
}

private const val TAG = "BillingServiceConnection"
