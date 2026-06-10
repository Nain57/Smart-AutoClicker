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

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModel
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModelState

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AreRequiredAlphabetModelsInstalledUseCaseTest {

    private val mockGetRequiredAlphabetModelsUseCase: GetRequiredAlphabetModelsUseCase = mockk()

    private lateinit var useCase: AreRequiredAlphabetModelsInstalledUseCase

    @Before
    fun setUp() {
        useCase = AreRequiredAlphabetModelsInstalledUseCase(mockGetRequiredAlphabetModelsUseCase)
    }

    @Test
    fun `no required models returns true`() = runTest {
        mockRequiredModels(emptyList())

        assertTrue(useCase(SCENARIO_ID).first())
    }

    @Test
    fun `all required models installed returns true`() = runTest {
        mockRequiredModels(listOf(
            recognitionModel(OCRAlphabet.LATIN, OCRModelState.Installed("path/latin")),
            recognitionModel(OCRAlphabet.ARABIC, OCRModelState.Installed("path/arabic")),
        ))

        assertTrue(useCase(SCENARIO_ID).first())
    }

    @Test
    fun `one required model downloadable returns false`() = runTest {
        mockRequiredModels(listOf(
            recognitionModel(OCRAlphabet.LATIN, OCRModelState.Downloadable),
        ))

        assertFalse(useCase(SCENARIO_ID).first())
    }

    @Test
    fun `one required model downloading returns false`() = runTest {
        mockRequiredModels(listOf(
            recognitionModel(OCRAlphabet.LATIN, OCRModelState.Downloading(progress = 42)),
        ))

        assertFalse(useCase(SCENARIO_ID).first())
    }

    @Test
    fun `mix of installed and non-installed models returns false`() = runTest {
        mockRequiredModels(listOf(
            recognitionModel(OCRAlphabet.LATIN, OCRModelState.Installed("path/latin")),
            recognitionModel(OCRAlphabet.ARABIC, OCRModelState.Downloadable),
        ))

        assertFalse(useCase(SCENARIO_ID).first())
    }

    @Test
    fun `mix of installed and downloading models returns false`() = runTest {
        mockRequiredModels(listOf(
            recognitionModel(OCRAlphabet.LATIN, OCRModelState.Installed("path/latin")),
            recognitionModel(OCRAlphabet.ARABIC, OCRModelState.Downloading(progress = 10)),
        ))

        assertFalse(useCase(SCENARIO_ID).first())
    }

    // region helpers

    private fun mockRequiredModels(models: List<OCRModel.Recognition>) {
        every { mockGetRequiredAlphabetModelsUseCase(SCENARIO_ID) } returns flowOf(models)
    }

    private fun recognitionModel(alphabet: OCRAlphabet, state: OCRModelState): OCRModel.Recognition =
        OCRModel.Recognition(state = state, alphabet = alphabet)

    // endregion

    private companion object {
        const val SCENARIO_ID = 1L
    }
}
