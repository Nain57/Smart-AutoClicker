
package com.buzbuz.smartautoclicker.core.domain.model

import androidx.annotation.IntDef

/** Defines the detection type to apply to a condition. */
@IntDef(EXACT, WHOLE_SCREEN, IN_AREA)
@Retention(AnnotationRetention.SOURCE)
annotation class DetectionType
/** The condition must be detected at the exact same position. */
const val EXACT = 1
/** The condition can be detected anywhere on the screen. */
const val WHOLE_SCREEN = 2
/** The condition can be detected only in a given area. */
const val IN_AREA = 3
