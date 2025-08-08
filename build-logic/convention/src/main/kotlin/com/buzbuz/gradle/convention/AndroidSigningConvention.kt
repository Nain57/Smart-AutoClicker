
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.androidApp
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