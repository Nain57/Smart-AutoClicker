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
package com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk

import android.content.Context
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlin.math.min

internal class BillingServiceConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @Dispatcher(Main) dispatcherMain: CoroutineDispatcher,
) {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcherMain)

    /** How long before the data source tries to reconnect to Google play. */
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
    private var reconnectJob: Job? = null

    private var connectionListener: ((BillingClientProxy?) -> Unit)? = null
    private var monitoredProductId: String? = null
    private var productPurchaseListener: ((Purchase?) -> Unit)? = null
    private var clientProxy: BillingClientProxy? = null


    fun monitorConnection(
        productId: String,
        onConnectionChangedListener: (BillingClientProxy?) -> Unit,
        onNewPurchasesListener: (Purchase?) -> Unit,
    ) {
        if (clientProxy != null) return

        connectionListener = onConnectionChangedListener
        monitoredProductId = productId
        productPurchaseListener = onNewPurchasesListener

        connect()
    }

    private fun connect() {
        val productId = monitoredProductId ?: return
        val clientListener = productPurchaseListener ?: return

        // Documentation is unclear about what kind of error to expect here, just catch everything
        try {
            clientProxy?.client?.endConnection()
        } catch (ex: Exception) {
            Log.e(TAG, "Can't end connection with current client.", ex)
        }

        clientProxy = BillingClientProxy(context, productId, clientListener)
        try {
            clientProxy?.client?.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected(): Unit = onDisconnected()
                override fun onBillingSetupFinished(p0: BillingResult): Unit = onSetupResult(p0)
            })
        } catch (isex: IllegalStateException) {
            Log.e(TAG, "connectToBillingService", isex)
            retryBillingServiceConnectionWithExponentialBackoff()
        }
    }

    private fun onSetupResult(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        Log.d(TAG, "onBillingSetupFinished: $responseCode $debugMessage")

        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                connectionListener?.invoke(clientProxy)
            }
            else -> {
                retryBillingServiceConnectionWithExponentialBackoff()
            }
        }
    }

    private fun onDisconnected() {
        Log.i(TAG, "onBillingServiceDisconnected")

        connectionListener?.invoke(null)
        retryBillingServiceConnectionWithExponentialBackoff()
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     */
    private fun retryBillingServiceConnectionWithExponentialBackoff() {
        if (reconnectJob != null) {
            Log.e(TAG, "Reconnection job is already running")
            return
        }

        reconnectJob = coroutineScopeMain.launch {
            delay(reconnectMilliseconds)
            reconnectMilliseconds = min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)

            reconnectJob = null
            connect()
        }
    }
}

private const val TAG = "BillingDataSource"
private const val RECONNECT_TIMER_START_MILLISECONDS = 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
