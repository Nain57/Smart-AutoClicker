/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import android.content.Intent
import android.os.Bundle

internal object LocalePluginContract {
    const val ACTION_EDIT_SETTING = "com.twofortyfouram.locale.intent.action.EDIT_SETTING"
    const val ACTION_EDIT_CONDITION = "com.twofortyfouram.locale.intent.action.EDIT_CONDITION"
    const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    const val ACTION_QUERY_CONDITION = "com.twofortyfouram.locale.intent.action.QUERY_CONDITION"
    const val ACTION_REQUEST_QUERY = "com.twofortyfouram.locale.intent.action.REQUEST_QUERY"
    const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
    const val EXTRA_STRING_JSON = "com.twofortyfouram.locale.intent.extra.STRING_JSON"
    const val EXTRA_STRING_ACTIVITY_CLASS_NAME = "com.twofortyfouram.locale.intent.extra.ACTIVITY"
    const val EXTRA_RELEVANT_VARIABLES = "net.dinglisch.android.tasker.RELEVANT_VARIABLES"
    const val EXTRA_VARIABLES_BUNDLE = "net.dinglisch.android.tasker.extras.VARIABLES"
    const val EXTRA_CONDITION_RESULT_RECEIVER = "net.dinglisch.android.tasker.EXTRA_RESULT_RECEIVER"

    fun readConfigurationJson(intent: Intent?): String? =
        intent?.getBundleExtra(EXTRA_BUNDLE)?.getString(EXTRA_STRING_JSON)

    fun createResult(configurationJson: String, blurb: String): Intent =
        Intent()
            .putExtra(EXTRA_BUNDLE, Bundle().apply { putString(EXTRA_STRING_JSON, configurationJson) })
            .putExtra(EXTRA_STRING_BLURB, blurb)

    fun createConditionResult(configurationJson: String, blurb: String, relevantVariables: Array<String>): Intent =
        createResult(configurationJson, blurb)
            .putExtra(EXTRA_RELEVANT_VARIABLES, relevantVariables)

    fun createRequestQueryIntent(configurationActivityClassName: String): Intent =
        Intent(ACTION_REQUEST_QUERY)
            .putExtra(EXTRA_STRING_ACTIVITY_CLASS_NAME, configurationActivityClassName)
}
