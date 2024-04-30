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
package com.buzbuz.smartautoclicker.feature.revenue.data.ads

internal sealed class RemoteAdState {

    data object SdkNotInitialized : RemoteAdState()
    data object Initialized : RemoteAdState()
    data object Loading : RemoteAdState()
    data object NotShown : RemoteAdState()
    data object Showing : RemoteAdState()
    data object Shown : RemoteAdState()

    sealed class Error : RemoteAdState() {
        abstract val code: Int?
        abstract val message: String?

        data class LoadingError(override val code: Int, override val message: String) : Error()
        data class ShowError(override val code: Int, override val message: String) : Error()
        data object NoImpressionError : Error() {
            override val code = null
            override val message = null
        }
    }
}
