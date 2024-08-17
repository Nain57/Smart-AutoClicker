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
package com.buzbuz.smartautoclicker.core.base.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt
fun Context.getThemeColor(@AttrRes colorAttr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(colorAttr, typedValue, true)
    return typedValue.data
}


fun Context.startWebBrowserActivity(url: String): Boolean =
    startWebBrowserActivity(Uri.parse(url))

fun Context.startWebBrowserActivity(uri: Uri): Boolean =
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        true
    } catch (ex: ActivityNotFoundException) {
        Log.e(TAG, "Can't open web browser.")
        startWebBrowserPicker(uri)
    }

private fun Context.startWebBrowserPicker(uri: Uri): Boolean =
    try {
        startActivity(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
                data = uri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        true
    } catch (ex: ActivityNotFoundException) {
        Log.e(TAG, "Can't open web browser.")
        false
    }

private const val TAG = "ContextExt"