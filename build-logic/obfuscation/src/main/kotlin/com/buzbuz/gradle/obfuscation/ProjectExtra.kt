
package com.buzbuz.gradle.obfuscation

import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra


internal fun Project.sourceFolderPath(): String =
    layout.projectDirectory.toString()


internal fun Project.setExtraOriginalApplicationId(appId: String): Unit =
    rootProject.extra.set(EXTRA_APPLICATION_ID, appId)
fun Project.getExtraOriginalApplicationId(): String =
    rootProject.extra.get(EXTRA_APPLICATION_ID) as String

internal fun Project.setExtraActualApplicationId(appId: String): Unit =
    rootProject.extra.set(EXTRA_ACTUAL_APPLICATION_ID, appId)
fun Project.getExtraActualApplicationId(): String =
    rootProject.extra.get(EXTRA_ACTUAL_APPLICATION_ID) as String


private const val EXTRA_APPLICATION_ID = "obfuscation_applicationId"
private const val EXTRA_ACTUAL_APPLICATION_ID = "obfuscation_actual_applicationId"
