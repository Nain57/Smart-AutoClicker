
package com.buzbuz.smartautoclicker.core.base.extensions

import android.util.Log


fun Exception.throwWithKeys(keys: Map<String, String>) {
    keys.entries.forEach { (key, value) ->
        Log.e("Exception", "Crash key $key = $value")
    }
    throw this
}