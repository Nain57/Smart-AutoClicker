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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.data

import android.content.Context
import android.os.Build

import androidx.test.core.app.ApplicationProvider

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TutorialCompletionStateDataSourceTest {

    @Mock private lateinit var mockGameRules: com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameRules

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var dataSource: TutorialCompletionStateDataSource
    private lateinit var mockCloseable: AutoCloseable

    @Before
    fun setUp() {
        mockCloseable = MockitoAnnotations.openMocks(this)
        dataSource = TutorialCompletionStateDataSource(
            PreferencesDataStore(
                context = context,
                dispatcher = testDispatcher,
                fileName = UUID.randomUUID().toString(),
            )
        )
    }

    // --- helpers ---

    private fun makeTutorial(id: String): Tutorial =
        Tutorial(
            info = TutorialInfo(id = id, nameResId = 0, descResId = 0),
            subject = TutorialSubject.QuickClickGame(
                instructionsResId = 0,
                scoreToReach = 3,
                durationSeconds = 10L,
                rules = mockGameRules,
            ),
            steps = emptyList(),
        )

    // --- getTutorialsCompletionState ---

    @Test
    fun `getTutorialsCompletionState returns empty map by default`() = runTest(testDispatcher) {
        val result = dataSource.getTutorialsCompletionState().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `setTutorialCompleted adds tutorial id with true to the map`() = runTest(testDispatcher) {
        val tutorial = makeTutorial("tutorial_1")

        dataSource.setTutorialCompleted(tutorial)

        val result = dataSource.getTutorialsCompletionState().first()
        assertTrue(result["tutorial_1"] == true)
    }

    @Test
    fun `setTutorialCompleted for multiple tutorials tracks all of them`() = runTest(testDispatcher) {
        val tutorial1 = makeTutorial("tutorial_1")
        val tutorial2 = makeTutorial("tutorial_2")

        dataSource.setTutorialCompleted(tutorial1)
        dataSource.setTutorialCompleted(tutorial2)

        val result = dataSource.getTutorialsCompletionState().first()
        assertEquals(2, result.size)
        assertTrue(result["tutorial_1"] == true)
        assertTrue(result["tutorial_2"] == true)
    }

    @Test
    fun `getTutorialsCompletionState does not include uncompleted tutorials`() = runTest(testDispatcher) {
        val tutorial1 = makeTutorial("tutorial_1")
        dataSource.setTutorialCompleted(tutorial1)

        val result = dataSource.getTutorialsCompletionState().first()
        assertFalse(result.containsKey("tutorial_2"))
    }

    @Test
    fun `setTutorialCompleted is idempotent`() = runTest(testDispatcher) {
        val tutorial = makeTutorial("tutorial_1")

        dataSource.setTutorialCompleted(tutorial)
        dataSource.setTutorialCompleted(tutorial)

        val result = dataSource.getTutorialsCompletionState().first()
        assertEquals(1, result.size)
        assertTrue(result["tutorial_1"] == true)
    }
}
