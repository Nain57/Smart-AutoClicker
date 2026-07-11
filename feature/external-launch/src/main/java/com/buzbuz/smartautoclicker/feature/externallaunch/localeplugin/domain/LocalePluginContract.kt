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
    const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
    const val EXTRA_STRING_JSON = "com.twofortyfouram.locale.intent.extra.STRING_JSON"

    fun readConfigurationJson(intent: Intent?): String? =
        intent?.getBundleExtra(EXTRA_BUNDLE)?.getString(EXTRA_STRING_JSON)

    fun createResult(configurationJson: String, blurb: String): Intent =
        Intent()
            .putExtra(EXTRA_BUNDLE, Bundle().apply { putString(EXTRA_STRING_JSON, configurationJson) })
            .putExtra(EXTRA_STRING_BLURB, blurb)
}
