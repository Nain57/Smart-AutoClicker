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
package com.buzbuz.smartautoclicker.feature.review.engine.session

internal data class UserSession(
    val timestamp: Long,
    val durationMs: Long,
)

internal fun List<UserSession>.toPreferences(): Set<String> =
    buildSet {
        this@toPreferences.forEach { userSession ->
            add(userSession.toPreference())
        }
    }

internal fun Set<String>.toUserSessions(): List<UserSession> =
    map { stringSession -> stringSession.toUserSession() }

private fun UserSession.toPreference(): String =
    "$timestamp$FIELD_DELIMITER$durationMs"

private fun String.toUserSession(): UserSession {
    val fields = split(FIELD_DELIMITER)
    if (fields.size != 2) throw IllegalStateException("Invalid user session string $this")

    return UserSession(
        timestamp = fields[0].toLong(),
        durationMs = fields[1].toLong(),
    )
}



private const val FIELD_DELIMITER = "|"