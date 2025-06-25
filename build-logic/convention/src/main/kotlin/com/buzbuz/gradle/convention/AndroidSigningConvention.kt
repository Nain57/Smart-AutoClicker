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
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.extensions.androidApp
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidSigningConvention : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        androidApp {
            buildTypes.forEach { buildType ->
                val signConfig = signingConfigs.findByName(buildType.name) ?: return@forEach

                if (signConfig.isSigningReady) {
                    if (signConfig.storeFile?.exists() == true) {
                        buildType.signingConfig = signConfig
                    } else {
                        logger.warn("WARNING: Signing store file is missing, ${buildType.name} apk can't be signed")
                    }
                } else {
                    logger.warn("WARNING: Signing config is incomplete, ${buildType.name} apk can't be signed")
                }
            }
        }
    }
}