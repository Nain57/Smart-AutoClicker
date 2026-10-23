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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue

import com.buzbuz.smartautoclicker.core.base.extensions.logCrashBreadcrumb
import com.buzbuz.smartautoclicker.core.base.extensions.recordNonFatal
import com.buzbuz.smartautoclicker.core.base.extensions.setCrashKey

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class BillingIssueReporter @Inject constructor() {

    private val reportedIssues: MutableSet<String> = mutableSetOf()

    /**
     * Keep a billing issue as context for the next Crashlytics report, and send it as a non fatal if it is worth one.
     * Each issue/code pair is sent once per process.
     */
    fun report(issue: BillingIssue, responseCode: Int? = null, debugMessage: String? = null) {
        val issueKey = "$issue/$responseCode"
        logCrashBreadcrumb("Billing issue $issueKey: $debugMessage")
        setCrashKey(KEY_LAST_BILLING_ISSUE, issueKey)

        if (!issue.isNonFatal(responseCode)) return
        synchronized(reportedIssues) {
            if (!reportedIssues.add(issueKey)) return
        }

        BillingIssueException(issue, "code=$responseCode; message=$debugMessage").recordNonFatal()
    }
}

/**
 * Crashlytics groups issues by stack trace, and every exception is created here. The synthetic top frame gives each
 * [BillingIssue] its own Crashlytics issue, while the response codes stay variants of it.
 */
private class BillingIssueException(issue: BillingIssue, message: String) : Exception("$issue: $message") {
    init {
        stackTrace = arrayOf(StackTraceElement(BillingIssue::class.java.name, issue.name, null, -1)) + stackTrace
    }
}

private const val KEY_LAST_BILLING_ISSUE = "billing_last_issue"
