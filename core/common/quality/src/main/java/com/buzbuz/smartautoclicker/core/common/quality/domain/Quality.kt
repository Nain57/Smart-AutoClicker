
package com.buzbuz.smartautoclicker.core.common.quality.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Describe the different quality levels felt by the user. */
sealed class Quality(internal val backToHighDelay: Duration? = null) {

    /** The quality is not initialized yet. */
    data object Unknown : Quality()

    /** Everything is working as intended. */
    data object High : Quality()

    /** The issue has occurred due to external perturbations, such as aggressive background service management */
    data object ExternalIssue : Quality(5.minutes)

    /** The issue has occurred due to a crash of Smart AutoClicker. */
    data object Crashed : Quality(1.hours)

    /** The user is using the app for the first time. */
    data object FirstTime : Quality(30.minutes)
}