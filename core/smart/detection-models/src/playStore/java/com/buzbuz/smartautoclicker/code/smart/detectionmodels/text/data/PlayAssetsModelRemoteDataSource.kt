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
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.data.RecognitionModelLocalDataSource
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.data.RecognitionModelsRemoteDataSource
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PlayAssetsModelRemoteDataSource @Inject constructor(
    @ApplicationContext context: Context,
    private val localDataSource: RecognitionModelLocalDataSource,
): RecognitionModelsRemoteDataSource {

    private val assetPackManager: AssetPackManager = AssetPackManagerFactory.getInstance(context)

    override suspend fun downloadRecognitionModel(alphabet: OCRAlphabet) {
        downloadPack("models_${alphabet.name.lowercase()}", alphabet)
    }

    private suspend fun downloadPack(packName: String, alphabet: OCRAlphabet) {
        assetPackManager.fetch(listOf(packName)).await()

        val location = assetPackManager.getPackLocation(packName)
        if (location != null) {
            val archiveName = "${alphabet.name.lowercase()}.tar.gz"
            val archiveFile = File(location.assetsPath(), archiveName)
            if (archiveFile.exists()) {
                archiveFile.inputStream().use { input ->
                    localDataSource.saveAndExtractModel(alphabet, input)
                }
            }
        }
    }
}
