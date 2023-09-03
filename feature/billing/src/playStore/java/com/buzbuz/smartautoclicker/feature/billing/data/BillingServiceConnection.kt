/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.billing.data

import android.os.Handler
import android.os.Looper
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult

import kotlin.math.min

internal class BillingServiceConnection(
    private val billingClient: BillingClient,
    private val onBillingClientConnected: () -> Unit,
): BillingClientStateListener {

    /** Main thread handler. */
    private val handler = Handler(Looper.getMainLooper())
    /** How long before the data source tries to reconnect to Google play. */
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        Log.d(TAG, "onBillingSetupFinished: $responseCode $debugMessage")

        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // The billing client is ready. You can query purchases here.
                // This doesn't mean that your app is set up correctly in the console -- it just
                // means that you have a connection to the Billing service.
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                onBillingClientConnected()
            }
            else -> retryBillingServiceConnectionWithExponentialBackoff()
        }
    }

    override fun onBillingServiceDisconnected() {
        retryBillingServiceConnectionWithExponentialBackoff()
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     */
    private fun retryBillingServiceConnectionWithExponentialBackoff() {
        handler.postDelayed({
            try {
                billingClient.startConnection(this@BillingServiceConnection)
            } catch (isex: IllegalStateException) {
                Log.e(TAG, "connectToBillingService", isex)
            }
        }, reconnectMilliseconds)
        reconnectMilliseconds = min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)
    }
}

private const val TAG = "BillingServiceConnection"
private const val RECONNECT_TIMER_START_MILLISECONDS = 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
