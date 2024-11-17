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

import com.buzbuz.smartautoclicker.core.base.data.getOpenWebBrowserIntent
import com.buzbuz.smartautoclicker.core.base.data.getOpenWebBrowserPickerIntent

@ColorInt
fun Context.getThemeColor(@AttrRes colorAttr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(colorAttr, typedValue, true)
    return typedValue.data
}


fun Context.safeStartWebBrowserActivity(url: String): Boolean =
    safeStartWebBrowserActivity(Uri.parse(url))

fun Context.safeStartWebBrowserActivity(uri: Uri): Boolean {
    if (safeStartActivity(getOpenWebBrowserIntent(uri))) return true
    return safeStartActivity(getOpenWebBrowserPickerIntent(uri))
}

fun Context.safeStartActivity(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (ex: ActivityNotFoundException) {
        Log.e(TAG, "No activity found to handle startActivity with $intent.")
        false
    } catch (secEx: SecurityException) {
        Log.e(TAG, "Not allowed to startActivity with $intent.")
        false
    } catch (ex: Exception) {
        Log.e(TAG, "Error while startActivity with $intent")
        false
    }

private const val TAG = "ContextExt"