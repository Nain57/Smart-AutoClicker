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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.subject

import android.graphics.PointF
import android.graphics.Rect
import android.os.Build

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TutorialGameEngineTest {

    companion object {
        private const val TEST_DURATION_SECONDS = 5L
        private const val TEST_SCORE_TO_REACH = 3
        private val TEST_AREA = Rect(0, 0, 1080, 1920)
        private val INITIAL_TARGETS = mapOf(TutorialGameTargetType.IMAGE_BLUE to PointF(100f, 100f))
        private val TICK_TARGETS = mapOf(TutorialGameTargetType.IMAGE_BLUE to PointF(200f, 200f))
        private val HIT_TARGETS = mapOf(TutorialGameTargetType.IMAGE_RED to PointF(300f, 300f))
    }

    @Mock private lateinit var mockRules: TutorialGameRules

    private lateinit var game: TutorialSubject.Game
    private lateinit var engine: TutorialGameEngine

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        whenever(mockRules.onStart(any())).thenReturn(INITIAL_TARGETS)
        whenever(mockRules.onTimerTick(any(), any())).thenReturn(TICK_TARGETS)
        whenever(mockRules.onValidTargetHit(any(), any())).thenReturn(HIT_TARGETS)
        whenever(mockRules.getScore()).thenReturn(0)

        game = TutorialSubject.Game(
            instructionsResId = 0,
            scoreToReach = TEST_SCORE_TO_REACH,
            durationSeconds = TEST_DURATION_SECONDS,
            rules = mockRules,
        )
    }

    private fun createEngine(dispatcher: kotlinx.coroutines.CoroutineDispatcher): TutorialGameEngine =
        TutorialGameEngine(dispatcher, game)

    // --- startGame ---

    @Test
    fun `startGame sets timeLeft to game duration`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1) // let coroutine launch and reach first state update

        assertEquals(TEST_DURATION_SECONDS, engine.state.value.timeLeft)
    }

    @Test
    fun `startGame sets initial targets from onStart`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1)

        assertEquals(INITIAL_TARGETS, engine.state.value.targets)
    }

    @Test
    fun `startGame when already running does not call onStart a second time`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1)
        engine.startGame(TEST_AREA) // second call should be no-op

        verify(mockRules, times(1)).onStart(any())
    }

    // --- timer loop ---

    @Test
    fun `timer loop decrements timeLeft after one second`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1_001) // past the first 1-second delay

        assertEquals(TEST_DURATION_SECONDS - 1, engine.state.value.timeLeft)
    }

    @Test
    fun `timer loop updates targets on each tick`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1_001)

        assertEquals(TICK_TARGETS, engine.state.value.targets)
    }

    @Test
    fun `timer loop updates score on each tick`() = runTest {
        whenever(mockRules.getScore()).thenReturn(2)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1_001)

        assertEquals(2, engine.state.value.score)
    }

    // --- game over ---

    @Test
    fun `game over sets isFinished to true`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        // advance past all ticks (duration * 1s each)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertTrue(engine.state.value.isFinished)
    }

    @Test
    fun `game over sets timeLeft to zero`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertEquals(0L, engine.state.value.timeLeft)
    }

    @Test
    fun `game over sets targets to empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertTrue(engine.state.value.targets.isEmpty())
    }

    @Test
    fun `game over with score greater than scoreToReach sets isWon to true`() = runTest {
        whenever(mockRules.getScore()).thenReturn(TEST_SCORE_TO_REACH + 1)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertTrue(engine.state.value.isWon == true)
    }

    @Test
    fun `game over with score equal to scoreToReach sets isWon to false`() = runTest {
        whenever(mockRules.getScore()).thenReturn(TEST_SCORE_TO_REACH)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertFalse(engine.state.value.isWon == true)
    }

    @Test
    fun `game over with score less than scoreToReach sets isWon to false`() = runTest {
        whenever(mockRules.getScore()).thenReturn(TEST_SCORE_TO_REACH - 1)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertFalse(engine.state.value.isWon == true)
    }

    // --- stop ---

    @Test
    fun `stop resets isFinished to false`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)
        engine.stop()

        assertFalse(engine.state.value.isFinished)
    }

    @Test
    fun `stop resets isWon to null`() = runTest {
        whenever(mockRules.getScore()).thenReturn(TEST_SCORE_TO_REACH + 1)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)
        engine.stop()

        assertNull(engine.state.value.isWon)
    }

    @Test
    fun `stop resets timeLeft to zero`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1_001)
        engine.stop()

        assertEquals(0L, engine.state.value.timeLeft)
    }

    @Test
    fun `stop resets targets to empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1_001)
        engine.stop()

        assertTrue(engine.state.value.targets.isEmpty())
    }

    @Test
    fun `stop cancels running game so isFinished stays false after idle`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1_001)
        engine.stop()

        // Advance past where the game would have ended naturally
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000)

        assertFalse(engine.state.value.isFinished)
    }

    // --- onGameTargetHit ---

    @Test
    fun `onGameTargetHit updates targets from onValidTargetHit`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1)

        engine.onGameTargetHit(TutorialGameTargetType.IMAGE_BLUE)

        assertEquals(HIT_TARGETS, engine.state.value.targets)
    }

    @Test
    fun `onGameTargetHit updates score`() = runTest {
        whenever(mockRules.getScore()).thenReturn(1)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        engine.startGame(TEST_AREA)
        advanceTimeBy(1)

        engine.onGameTargetHit(TutorialGameTargetType.IMAGE_BLUE)

        assertEquals(1, engine.state.value.score)
    }

    // --- monitorNextCompletion ---

    @Test
    fun `monitorNextCompletion callback is invoked with true when score exceeds target`() = runTest {
        whenever(mockRules.getScore()).thenReturn(TEST_SCORE_TO_REACH + 1)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        var callbackResult: Boolean? = null
        engine.monitorNextCompletion { isWon -> callbackResult = isWon }

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertEquals(true, callbackResult)
    }

    @Test
    fun `monitorNextCompletion callback is invoked with false when score does not exceed target`() = runTest {
        whenever(mockRules.getScore()).thenReturn(TEST_SCORE_TO_REACH - 1)

        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        var callbackResult: Boolean? = null
        engine.monitorNextCompletion { isWon -> callbackResult = isWon }

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertEquals(false, callbackResult)
    }

    @Test
    fun `monitorNextCompletion with null clears the callback`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        var callbackInvoked = false
        engine.monitorNextCompletion { callbackInvoked = true }
        engine.monitorNextCompletion(null)

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertFalse(callbackInvoked)
    }

    @Test
    fun `monitorNextCompletion callback is cleared after invocation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        engine = createEngine(dispatcher)

        var invokeCount = 0
        engine.monitorNextCompletion { invokeCount++ }

        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        // Start a second game — callback must not fire again
        engine.startGame(TEST_AREA)
        advanceTimeBy(TEST_DURATION_SECONDS * 1_000 + 500)

        assertEquals(1, invokeCount)
    }
}
