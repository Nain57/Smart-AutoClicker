
package com.buzbuz.smartautoclicker.core.base.extensions

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase


fun Exception.throwWithKeys(keys: Map<String, String>) {
    Firebase.crashlytics.setCustomKeys {
        keys.entries.forEach { (key, values) ->
            key(key, values)
        }
    }
    throw this
}