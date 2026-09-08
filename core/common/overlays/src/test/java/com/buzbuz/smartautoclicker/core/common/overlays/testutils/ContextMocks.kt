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
package com.buzbuz.smartautoclicker.core.common.overlays.testutils

import android.content.Context

import org.mockito.Mockito

/**
 * Mock a system service on a mocked [Context].
 *
 * An overlay context wraps the context it is created from, so a service can be requested either directly on the mock,
 * or through the [Context] wrapping mechanism. As [Context.getSystemService] with a [Class] is final, the latter is
 * resolved with [Context.getSystemServiceName] followed by [Context.getSystemService] with a name: all of them must be
 * mocked for the service to be found in both cases.
 *
 * @param serviceClass the class of the mocked service.
 * @param serviceName the name of the mocked service.
 * @param service the mock returned for this service.
 */
fun <T> Context.mockSystemService(serviceClass: Class<T>, serviceName: String, service: T) {
    Mockito.`when`(getSystemServiceName(serviceClass)).thenReturn(serviceName)
    Mockito.`when`(getSystemService(serviceClass)).thenReturn(service)
    Mockito.`when`(getSystemService(serviceName)).thenReturn(service)
}
