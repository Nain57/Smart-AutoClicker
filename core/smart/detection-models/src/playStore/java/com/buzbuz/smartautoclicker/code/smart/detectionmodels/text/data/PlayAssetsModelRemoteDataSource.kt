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

import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.plus

@Singleton
internal class PlayAssetsModelRemoteDataSource @Inject constructor(
    @ApplicationContext context: Context,
): RecognitionModelsRemoteDataSource {

    private val assetPackManager: AssetPackManager = AssetPackManagerFactory.getInstance(context)

    private val _currentlyDownloading: MutableStateFlow<Map<OCRAlphabet, Int>> = MutableStateFlow(emptyMap())
    override val currentlyDownloading: StateFlow<Map<OCRAlphabet, Int>> = _currentlyDownloading

    private val recognitionModelsDataDir: File = context.recognitionModelsDataDir()


    override suspend fun downloadRecognitionModel(alphabet: OCRAlphabet, onSuccess: () -> Unit) {
        if (currentlyDownloading.value.contains(alphabet)) {
            Log.w(TAG, "Ignore download request for $alphabet model")
            return
        }
        _currentlyDownloading.update { old -> old + (alphabet to 0) }

        val packName = alphabet.recognitionModelAssetPackName()
        val listener = AssetPackStateUpdateListener { state ->
            when (state.status()) {
                AssetPackStatus.DOWNLOADING -> {
                    val progress = (state.bytesDownloaded() * 100 / state.totalBytesToDownload()).toInt()
                    _currentlyDownloading.update { old -> old + (alphabet to progress) }
                }

                AssetPackStatus.TRANSFERRING ->
                    _currentlyDownloading.update { old -> old + (alphabet to 100) }

                AssetPackStatus.COMPLETED -> {
                    // Copy from asset pack location to data model folder
                    val packPath = assetPackManager.getPackLocation(packName)?.assetsPath()
                    if (packPath == null || !syncPackToFilesDir(alphabet, packPath)) {
                        Log.e(TAG, "Error while installing model $alphabet")
                        _currentlyDownloading.update { old -> old - alphabet }
                        return@AssetPackStateUpdateListener
                    }

                    _currentlyDownloading.update { old -> old - alphabet }
                    onSuccess()
                }

                AssetPackStatus.CANCELED -> {
                    Log.w(TAG, "Asset pack download canceled")
                    _currentlyDownloading.update { old -> old - alphabet }

                }

                else -> {
                    Log.w(TAG, "Asset pack download failed: ${state.errorCode()}")
                    _currentlyDownloading.update { old -> old - alphabet }
                }

            }
        }

        assetPackManager.registerListener(listener)
        assetPackManager.fetch(listOf(packName))
    }
    
    private fun syncPackToFilesDir(alphabet: OCRAlphabet, packAssetsPath: String): Boolean {
        val srcDir = File("$packAssetsPath/${alphabet.recognitionModelAssetDir()}")
        val destDir = recognitionModelsDataDir.recognitionModelDataDir(alphabet)

        try {
            destDir.mkdirs()
            srcDir.listFiles()?.forEach { srcFile ->
                srcFile.copyTo(File(destDir, srcFile.name), overwrite = true)
            }

            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Error while installing models file", ex)
            return false
        }
    }
}

private const val TAG = "PlayAssetsModelRemoteDataSource"