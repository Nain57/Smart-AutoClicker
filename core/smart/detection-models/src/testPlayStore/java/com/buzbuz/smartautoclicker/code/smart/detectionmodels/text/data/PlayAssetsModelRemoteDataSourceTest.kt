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
package com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.data

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet

import com.google.android.play.core.assetpacks.AssetPackLocation
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify

import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File

class PlayAssetsModelRemoteDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val listenerSlot = slot<AssetPackStateUpdateListener>()

    private lateinit var mockAssetPackManager: AssetPackManager
    private lateinit var dataSource: PlayAssetsModelRemoteDataSource

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any<Throwable>()) } returns 0

        mockkStatic(AssetPackManagerFactory::class)
        mockAssetPackManager = mockk {
            every { registerListener(capture(listenerSlot)) } just Runs
            every { unregisterListener(any()) } just Runs
            every { fetch(any()) } returns mockk(relaxed = true)
            every { getPackLocation(any()) } returns null
        }
        every { AssetPackManagerFactory.getInstance(any()) } returns mockAssetPackManager

        val mockContext = mockk<Context> {
            every { filesDir } returns tempFolder.root
        }
        dataSource = PlayAssetsModelRemoteDataSource(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `duplicate download request is ignored`() = runTest {
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}

        verify(exactly = 1) { mockAssetPackManager.fetch(any()) }
    }

    @Test
    fun `PENDING status keeps entry in map at 0`() = runTest {
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        listenerSlot.captured.onStateUpdate(mockState(AssetPackStatus.PENDING))

        assertEquals(0, dataSource.currentlyDownloading.value[TEST_ALPHABET])
    }

    @Test
    fun `DOWNLOADING status updates progress`() = runTest {
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        listenerSlot.captured.onStateUpdate(
            mockState(AssetPackStatus.DOWNLOADING, bytesDownloaded = 40L, totalBytes = 100L)
        )

        assertEquals(40, dataSource.currentlyDownloading.value[TEST_ALPHABET])
    }

    @Test
    fun `TRANSFERRING status sets progress to 100`() = runTest {
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        listenerSlot.captured.onStateUpdate(mockState(AssetPackStatus.TRANSFERRING))

        assertEquals(100, dataSource.currentlyDownloading.value[TEST_ALPHABET])
    }

    @Test
    fun `WAITING_FOR_WIFI keeps entry in map`() = runTest {
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        listenerSlot.captured.onStateUpdate(mockState(AssetPackStatus.WAITING_FOR_WIFI))

        assertEquals(0, dataSource.currentlyDownloading.value[TEST_ALPHABET])
    }

    @Test
    fun `COMPLETED with null pack location does not call onSuccess`() = runTest {
        every { mockAssetPackManager.getPackLocation(any()) } returns null

        var onSuccessCalled = false
        dataSource.downloadRecognitionModel(TEST_ALPHABET) { onSuccessCalled = true }
        listenerSlot.captured.onStateUpdate(mockState(AssetPackStatus.COMPLETED))

        assertFalse(onSuccessCalled)
        assertNull(dataSource.currentlyDownloading.value[TEST_ALPHABET])
    }

    @Test
    fun `COMPLETED with valid pack location calls onSuccess and removes entry`() = runTest {
        setupAssetPackFiles(TEST_ALPHABET)

        var onSuccessCalled = false
        dataSource.downloadRecognitionModel(TEST_ALPHABET) { onSuccessCalled = true }
        listenerSlot.captured.onStateUpdate(mockState(AssetPackStatus.COMPLETED))

        assertTrue(onSuccessCalled)
        assertNull(dataSource.currentlyDownloading.value[TEST_ALPHABET])
    }

    @Test
    fun `COMPLETED copies files to the recognition model data dir`() = runTest {
        setupAssetPackFiles(TEST_ALPHABET, MODEL_FILES)

        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        listenerSlot.captured.onStateUpdate(mockState(AssetPackStatus.COMPLETED))

        val destDir = File(tempFolder.root, TEST_ALPHABET.recognitionModelDataDirPath())
        MODEL_FILES.forEach { file ->
            assertTrue("$file not found in destination", File(destDir, file).exists())
        }
    }

    @Test
    fun `COMPLETED unregisters listener`() = runTest {
        setupAssetPackFiles(TEST_ALPHABET)

        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        val capturedListener = listenerSlot.captured
        capturedListener.onStateUpdate(mockState(AssetPackStatus.COMPLETED))

        verify { mockAssetPackManager.unregisterListener(capturedListener) }
    }

    @Test
    fun `CANCELED removes entry from map and unregisters listener`() = runTest {
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        val capturedListener = listenerSlot.captured
        capturedListener.onStateUpdate(mockState(AssetPackStatus.CANCELED))

        assertNull(dataSource.currentlyDownloading.value[TEST_ALPHABET])
        verify { mockAssetPackManager.unregisterListener(capturedListener) }
    }

    @Test
    fun `FAILED removes entry from map and unregisters listener`() = runTest {
        dataSource.downloadRecognitionModel(TEST_ALPHABET) {}
        val capturedListener = listenerSlot.captured
        capturedListener.onStateUpdate(mockState(AssetPackStatus.FAILED))

        assertNull(dataSource.currentlyDownloading.value[TEST_ALPHABET])
        verify { mockAssetPackManager.unregisterListener(capturedListener) }
    }

    // region helpers

    private fun setupAssetPackFiles(alphabet: OCRAlphabet, files: List<String> = emptyList()) {
        val packRoot = tempFolder.newFolder("asset_pack")
        val assetDir = File(packRoot, alphabet.recognitionModelAssetDir()).also { it.mkdirs() }
        files.forEach { File(assetDir, it).createNewFile() }

        val mockLocation = mockk<AssetPackLocation> {
            every { assetsPath() } returns packRoot.absolutePath
        }
        every { mockAssetPackManager.getPackLocation(alphabet.recognitionModelAssetPackName()) } returns mockLocation
    }

    private fun mockState(
        status: Int,
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 100L,
        errorCode: Int = 0,
    ): AssetPackState = mockk {
        every { status() } returns status
        every { bytesDownloaded() } returns bytesDownloaded
        every { totalBytesToDownload() } returns totalBytes
        every { errorCode() } returns errorCode
    }

    // endregion

    private companion object {
        val TEST_ALPHABET = OCRAlphabet.ARABIC
        val MODEL_FILES = listOf(
            OCR_RECOGNITION_MODEL_FILE,
            OCR_RECOGNITION_MODEL_PARAMS_FILE,
            OCR_RECOGNITION_MODEL_DICTIONARY_FILE,
        )
    }
}
