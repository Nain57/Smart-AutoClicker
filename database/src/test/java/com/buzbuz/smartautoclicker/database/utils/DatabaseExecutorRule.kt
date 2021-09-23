/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.utils

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.DefaultTaskExecutor
import androidx.arch.core.executor.TaskExecutor

import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Junit Rule allowing to test a RoomDatabase synchronously.
 *
 * To test a RoomDatabase in Android Local tests, we must execute all request synchronously in order to verify all
 * conditions before exiting the [org.junit.Test] method. This can be accomplished by using the
 * [org.robolectric.RobolectricTestRunner] as your [org.junit.runner.RunWith] for your test class and do the following
 * steps:
 *
 * * First set this class a Junit Rule using [org.junit.Rule]. In kotlin, you can add the following to your test class:
 * ```
 * @get:Rule val databaseExecutorRule = DatabaseExecutorRule()
 * ```
 *
 * * Then, you need to add a [kotlinx.coroutines.test.TestCoroutineDispatcher] and a
 * [kotlinx.coroutines.test.TestCoroutineScope] as members of your test class. Those members will be used in the
 * init and clean methods of your tests, as following:
 * ```
 * private val testDispatcher = TestCoroutineDispatcher()
 * private val testScope = TestCoroutineScope(testDispatcher)
 *
 * @Before
 * fun setUp() {
 *     Dispatchers.setMain(testDispatcher)
 * }
 * @After
 * fun tearDown() {
 *     testScope.cleanupTestCoroutines()
 *     Dispatchers.resetMain()
 * }
 * ```
 *
 * * You might want to use a database in RAM instead of on a disk, as you are executing the tests on your local JVM.
 * On that database, as we will execute all queries on the main thread, we will need to allow it using
 * [androidx.room.RoomDatabase.Builder.allowMainThreadQueries]. Simply add the following to the init and clean methods:
 * ```
 * @Before
 * fun setUp() {
 *     ...
 *     database = Room.inMemoryDatabaseBuilder(
 *                         InstrumentationRegistry.getInstrumentation().targetContext,
 *                         MyDb::class.java
 *                     )
 *                    .allowMainThreadQueries()
 *                    .build()
 * }
 * @After
 * fun tearDown() {
 *     database.clearAllTables()
 *     database.close()
 *     ...
 * }
 * ```
 *
 * * Finally, all your test must be executed in a [kotlinx.coroutines.runBlocking] block, such as:
 * ```
 * fun myTestFunction() = runBlocking {
 *     // My test code
 * }
 * ```
 */
class DatabaseExecutorRule : TestWatcher() {

    val defaultExecutor = DefaultTaskExecutor()

    override fun starting(description: Description?) {
        super.starting(description)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                defaultExecutor.executeOnDiskIO(runnable)
            }

            override fun postToMainThread(runnable: Runnable) {
                defaultExecutor.executeOnDiskIO(runnable)
            }

            override fun isMainThread(): Boolean {
                return true
            }
        })
    }

    override fun finished(description: Description?) {
        super.finished(description)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}