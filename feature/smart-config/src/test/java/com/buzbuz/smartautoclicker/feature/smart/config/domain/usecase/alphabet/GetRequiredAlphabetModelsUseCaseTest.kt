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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.alphabet

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.OCRModelsRepository
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModel
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModelState
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetRequiredAlphabetModelsUseCaseTest {

    private val mockRepository: IRepository = mockk()
    private val mockOcrModelsRepository: OCRModelsRepository = mockk()

    private lateinit var useCase: GetRequiredAlphabetModelsUseCase

    @Before
    fun setUp() {
        useCase = GetRequiredAlphabetModelsUseCase(mockRepository, mockOcrModelsRepository)
    }

    @Test
    fun `no events emits empty list`() = runTest {
        mockEvents(emptyList())
        mockAvailableModels(setOf(recognitionModel(OCRAlphabet.LATIN)))

        val result = useCase(SCENARIO_ID).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `events with no text conditions emits empty list`() = runTest {
        val imageCondition = mockk<ScreenCondition.Image>(relaxed = true)
        val colorCondition = mockk<ScreenCondition.Color>(relaxed = true)
        mockEvents(listOf(screenEvent(conditions = listOf(imageCondition, colorCondition))))
        mockAvailableModels(setOf(recognitionModel(OCRAlphabet.LATIN)))

        val result = useCase(SCENARIO_ID).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `single text condition returns its matching model`() = runTest {
        val textCondition = textCondition(OCRAlphabet.LATIN)
        mockEvents(listOf(screenEvent(conditions = listOf(textCondition))))
        val latinModel = recognitionModel(OCRAlphabet.LATIN)
        mockAvailableModels(setOf(latinModel, recognitionModel(OCRAlphabet.ARABIC)))

        val result = useCase(SCENARIO_ID).first()

        assertEquals(listOf(latinModel), result)
    }

    @Test
    fun `text conditions across multiple events returns all matching models`() = runTest {
        mockEvents(listOf(
            screenEvent(conditions = listOf(textCondition(OCRAlphabet.LATIN))),
            screenEvent(conditions = listOf(textCondition(OCRAlphabet.ARABIC))),
        ))
        val latinModel = recognitionModel(OCRAlphabet.LATIN)
        val arabicModel = recognitionModel(OCRAlphabet.ARABIC)
        mockAvailableModels(setOf(latinModel, arabicModel, recognitionModel(OCRAlphabet.JAPANESE)))

        val result = useCase(SCENARIO_ID).first()

        assertEquals(2, result.size)
        assertTrue(result.containsAll(listOf(latinModel, arabicModel)))
    }

    @Test
    fun `duplicate alphabets across events are deduplicated`() = runTest {
        mockEvents(listOf(
            screenEvent(conditions = listOf(textCondition(OCRAlphabet.LATIN))),
            screenEvent(conditions = listOf(textCondition(OCRAlphabet.LATIN))),
        ))
        val latinModel = recognitionModel(OCRAlphabet.LATIN)
        mockAvailableModels(setOf(latinModel))

        val result = useCase(SCENARIO_ID).first()

        assertEquals(listOf(latinModel), result)
    }

    @Test
    fun `duplicate alphabets within same event are deduplicated`() = runTest {
        mockEvents(listOf(
            screenEvent(conditions = listOf(textCondition(OCRAlphabet.LATIN), textCondition(OCRAlphabet.LATIN))),
        ))
        val latinModel = recognitionModel(OCRAlphabet.LATIN)
        mockAvailableModels(setOf(latinModel))

        val result = useCase(SCENARIO_ID).first()

        assertEquals(listOf(latinModel), result)
    }

    @Test
    fun `text condition with no matching model in repository is skipped`() = runTest {
        mockEvents(listOf(screenEvent(conditions = listOf(textCondition(OCRAlphabet.JAPANESE)))))
        mockAvailableModels(setOf(recognitionModel(OCRAlphabet.LATIN)))

        val result = useCase(SCENARIO_ID).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `model state is preserved in result`() = runTest {
        mockEvents(listOf(screenEvent(conditions = listOf(textCondition(OCRAlphabet.LATIN)))))
        val downloadingModel = recognitionModel(OCRAlphabet.LATIN, OCRModelState.Downloading(50))
        mockAvailableModels(setOf(downloadingModel))

        val result = useCase(SCENARIO_ID).first()

        assertEquals(listOf(downloadingModel), result)
    }

    @Test
    fun `mixed condition types only includes text alphabets`() = runTest {
        val textCondition = textCondition(OCRAlphabet.ARABIC)
        val imageCondition = mockk<ScreenCondition.Image>(relaxed = true)
        val colorCondition = mockk<ScreenCondition.Color>(relaxed = true)
        mockEvents(listOf(screenEvent(conditions = listOf(imageCondition, textCondition, colorCondition))))
        val arabicModel = recognitionModel(OCRAlphabet.ARABIC)
        mockAvailableModels(setOf(arabicModel, recognitionModel(OCRAlphabet.LATIN)))

        val result = useCase(SCENARIO_ID).first()

        assertEquals(listOf(arabicModel), result)
    }

    // region helpers

    private fun mockEvents(events: List<ScreenEvent>) {
        every { mockRepository.getScreenEventsFlow(SCENARIO_ID) } returns flowOf(events)
    }

    private fun mockAvailableModels(models: Set<OCRModel.Recognition>) {
        every { mockOcrModelsRepository.recognitionModels } returns flowOf(models)
    }

    private fun screenEvent(conditions: List<ScreenCondition> = emptyList()): ScreenEvent =
        mockk(relaxed = true) { every { this@mockk.conditions } returns conditions }

    private fun textCondition(alphabet: OCRAlphabet): ScreenCondition.Text =
        mockk(relaxed = true) { every { this@mockk.alphabet } returns alphabet }

    private fun recognitionModel(
        alphabet: OCRAlphabet,
        state: OCRModelState = OCRModelState.Installed("path/$alphabet"),
    ): OCRModel.Recognition = OCRModel.Recognition(state = state, alphabet = alphabet)

    // endregion

    private companion object {
        const val SCENARIO_ID = 1L
    }
}
