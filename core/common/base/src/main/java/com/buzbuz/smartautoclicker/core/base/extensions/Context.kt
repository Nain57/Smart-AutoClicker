
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