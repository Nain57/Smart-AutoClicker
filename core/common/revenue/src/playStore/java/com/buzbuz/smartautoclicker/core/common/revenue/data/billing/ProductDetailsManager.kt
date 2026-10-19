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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing

import android.os.SystemClock
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.InAppProduct

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import javax.inject.Inject
import kotlin.time.Duration.Companion.hours


internal class ProductDetailsManager @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Details for the product*/
    private val _productDetails = MutableStateFlow<InAppProduct?>(null)
    val productDetails: StateFlow<InAppProduct?> = _productDetails

    /** When was the last successful ProductDetailsResponse */
    private var productDetailsResponseTime: Long? = null
    /** Job monitoring the product details requested with [startMonitoring]. */
    private var monitoringJob: Job? = null
    /** Serializes the refreshes, and guards [productDetailsResponseTime]. */
    private val refreshMutex = Mutex()

    suspend fun startMonitoring(refreshQuery: suspend () -> Pair<Boolean, InAppProduct?>) {
        monitoringJob?.cancel()

        // Refresh when someone is subscribed to the flow when nobody were
        monitoringJob = _productDetails.subscriptionCount
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .drop(1) // The current subscription state is covered by the refresh below
            .onEach { isActive -> if (isActive) refreshIfStale(refreshQuery) }
            .launchIn(coroutineScopeIo)

        refreshMutex.withLock { refresh(refreshQuery) }
    }

    private suspend fun refreshIfStale(refreshQuery: suspend () -> Pair<Boolean, InAppProduct?>) {
        refreshMutex.withLock {
            val lastResponseTime = productDetailsResponseTime
            if (lastResponseTime != null && SystemClock.elapsedRealtime() - lastResponseTime <= PRODUCT_DETAILS_QUERY_RETRY_TIME_MS) return

            Log.d(TAG, "Product to old, refreshing...")
            refresh(refreshQuery)
        }
    }

    private suspend fun refresh(refreshQuery: suspend () -> Pair<Boolean, InAppProduct?>) {
        val (success, product) = refreshQuery()
        if (success) {
            productDetailsResponseTime = SystemClock.elapsedRealtime()
            _productDetails.emit(product)
        } else {
            productDetailsResponseTime = null
        }
    }
}



private val PRODUCT_DETAILS_QUERY_RETRY_TIME_MS = 4.hours.inWholeMilliseconds
/** Tag for logs */
private const val TAG = "ProductDetailsManager"