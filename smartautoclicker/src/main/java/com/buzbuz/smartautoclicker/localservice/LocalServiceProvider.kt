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
package com.buzbuz.smartautoclicker.localservice

object LocalServiceProvider {

    /** The instance of the [ILocalService], providing access for this service to the Activity. */
    var localServiceInstance: ILocalService? = null
        private set(value) {
            field = value
            localServiceCallback?.invoke(field)
        }
    /** Callback upon the availability of the [localServiceInstance]. */
    private var localServiceCallback: ((ILocalService?) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(localServiceInstance)
        }

    fun setLocalService(service: ILocalService?) {
        localServiceInstance = service
    }
    /**
     * Static method allowing an activity to register a callback in order to monitor the availability of the
     * [ILocalService]. If the service is already available upon registration, the callback will be immediately
     * called.
     *
     * @param stateCallback the object to be notified upon service availability.
     */
    fun getLocalService(stateCallback: ((ILocalService?) -> Unit)?) {
        localServiceCallback = stateCallback
    }

    fun isServiceStarted(): Boolean = localServiceInstance != null
}