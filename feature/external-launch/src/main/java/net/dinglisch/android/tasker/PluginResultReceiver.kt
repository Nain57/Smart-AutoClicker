/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package net.dinglisch.android.tasker

import android.os.Handler
import android.os.ResultReceiver
import androidx.annotation.Keep

/** Compatibility class required by the Tasker condition result-receiver extension. */
@Keep
class PluginResultReceiver(handler: Handler?) : ResultReceiver(handler)
