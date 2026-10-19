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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing

import android.os.Build

import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.InAppProduct

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [ProductDetailsManager]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ProductDetailsManagerTests {

    private var queryCount = 0
    private var querySuccess = true

    private suspend fun query(): Pair<Boolean, InAppProduct?> {
        queryCount++
        delay(QUERY_DURATION_MS)
        return if (querySuccess) true to InAppProduct.Debug() else false to null
    }

    private fun TestScope.subscribe(manager: ProductDetailsManager): Job =
        launch { manager.productDetails.collect { } }

    @Test
    fun `start monitoring with a subscriber queries once`() = runTest {
        val manager = ProductDetailsManager(StandardTestDispatcher(testScheduler))
        val subscriber = subscribe(manager)
        advanceUntilIdle()

        launch { manager.startMonitoring(::query) }
        advanceUntilIdle()

        assertEquals(1, queryCount)
        subscriber.cancel()
    }

    @Test
    fun `new subscriber after a successful query does not query again`() = runTest {
        val manager = ProductDetailsManager(StandardTestDispatcher(testScheduler))
        launch { manager.startMonitoring(::query) }
        advanceUntilIdle()

        val subscriber = subscribe(manager)
        advanceUntilIdle()

        assertEquals(1, queryCount)
        subscriber.cancel()
    }

    @Test
    fun `new subscriber after a failed query queries again`() = runTest {
        querySuccess = false
        val manager = ProductDetailsManager(StandardTestDispatcher(testScheduler))
        launch { manager.startMonitoring(::query) }
        advanceUntilIdle()

        val subscriber = subscribe(manager)
        advanceUntilIdle()

        assertEquals(2, queryCount)
        subscriber.cancel()
    }
}

private const val QUERY_DURATION_MS = 100L
