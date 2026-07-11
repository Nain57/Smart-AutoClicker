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
package com.buzbuz.smartautoclicker.core.common.actions.external

import android.content.Intent
import android.os.Bundle

object ExternalActionEventContract {

    const val ACTION_EDIT_EVENT = "net.dinglisch.android.tasker.ACTION_EDIT_EVENT"
    const val ACTION_QUERY_CONDITION = "com.twofortyfouram.locale.intent.action.QUERY_CONDITION"
    const val ACTION_REQUEST_QUERY = "com.twofortyfouram.locale.intent.action.REQUEST_QUERY"

    const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
    const val EXTRA_STRING_JSON = "com.twofortyfouram.locale.intent.extra.STRING_JSON"
    const val EXTRA_STRING_ACTIVITY_CLASS_NAME = "com.twofortyfouram.locale.intent.extra.ACTIVITY"

    const val RESULT_CONDITION_SATISFIED = 16
    const val RESULT_CONDITION_UNSATISFIED = 17
    const val RESULT_CONDITION_UNKNOWN = 18

    private const val EXTRA_REQUEST_QUERY_PASS_THROUGH_DATA =
        "net.dinglisch.android.tasker.extras.PASS_THROUGH_DATA"

    private const val EXTRA_FIRED_ACTION_NAME =
        "com.buzbuz.smartautoclicker.extra.EXTERNAL_ACTION_NAME"

    const val EVENT_CONFIGURATION_ACTIVITY_CLASS_NAME =
        "com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui.ExternalActionEventConfigurationActivity"

    fun createConfigurationResult(configurationJson: String, blurb: String): Intent =
        Intent()
            .putExtra(EXTRA_BUNDLE, Bundle().apply { putString(EXTRA_STRING_JSON, configurationJson) })
            .putExtra(EXTRA_STRING_BLURB, blurb)

    fun readConfigurationJson(intent: Intent?): String? =
        intent?.getBundleExtra(EXTRA_BUNDLE)?.getString(EXTRA_STRING_JSON)

    fun createRequestQueryIntent(externalActionName: String): Intent =
        Intent(ACTION_REQUEST_QUERY)
            .putExtra(EXTRA_STRING_ACTIVITY_CLASS_NAME, EVENT_CONFIGURATION_ACTIVITY_CLASS_NAME)
            .putExtra(
                EXTRA_REQUEST_QUERY_PASS_THROUGH_DATA,
                Bundle().apply { putString(EXTRA_FIRED_ACTION_NAME, externalActionName.trim()) },
            )

    fun readFiredExternalActionName(intent: Intent?): String? =
        intent
            ?.getBundleExtra(EXTRA_REQUEST_QUERY_PASS_THROUGH_DATA)
            ?.getString(EXTRA_FIRED_ACTION_NAME)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}
