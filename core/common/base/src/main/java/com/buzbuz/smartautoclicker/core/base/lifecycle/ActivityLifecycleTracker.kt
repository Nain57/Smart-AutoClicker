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
package com.buzbuz.smartautoclicker.core.base.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle

import com.buzbuz.smartautoclicker.core.base.extensions.logCrashBreadcrumb
import com.buzbuz.smartautoclicker.core.base.extensions.setCrashKey

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the lifecycle of all activities in the application process, including the ones from external libraries.
 *
 * Platform crashes such as "Can't start activity that is not stopped" have no application frames in their stack
 * trace, so each lifecycle event is recorded as a crash breadcrumb in order to identify the faulty activity.
 */
@Singleton
class ActivityLifecycleTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {

    private val aliveActivities: MutableSet<Activity> = mutableSetOf()
    private val lastEvents: ArrayDeque<String> = ArrayDeque(MAX_EVENTS_IN_KEY)

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /** @return true if an activity of the application is alive and not finishing. */
    fun hasActiveActivity(): Boolean =
        aliveActivities.any { !it.isFinishing }

    /** Records an activity specific event, not covered by the lifecycle callbacks, in the crash breadcrumbs. */
    fun onCustomEvent(activity: Activity, event: String): Unit = onEvent(activity, event)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        aliveActivities.add(activity)
        onEvent(activity, "created")
    }

    override fun onActivityStarted(activity: Activity): Unit = onEvent(activity, "started")
    override fun onActivityResumed(activity: Activity): Unit = onEvent(activity, "resumed")
    override fun onActivityPaused(activity: Activity): Unit = onEvent(activity, "paused")
    override fun onActivityStopped(activity: Activity): Unit = onEvent(activity, "stopped")
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        aliveActivities.remove(activity)
        onEvent(activity, "destroyed")
    }

    private fun onEvent(activity: Activity, event: String) {
        val description = "${activity.javaClass.simpleName}@${System.identityHashCode(activity).toString(16)} " +
                "task=${activity.taskId} finishing=${activity.isFinishing} $event"
        logCrashBreadcrumb(description)

        if (lastEvents.size >= MAX_EVENTS_IN_KEY) lastEvents.removeFirst()
        lastEvents.addLast(description)
        setCrashKey(KEY_LAST_ACTIVITY_EVENTS, lastEvents.joinToString(separator = " | "))
    }
}

private const val KEY_LAST_ACTIVITY_EVENTS = "last_activity_events"
private const val MAX_EVENTS_IN_KEY = 8
