/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.actions.text

import java.util.regex.Pattern

fun String.appendCounterReference(counterName: String): String =
    "$this{$counterName}"

fun String.findCounterReferences(): List<String> {
    val counterReferences = mutableListOf<String>()
    // Looks for an opening brace {, then captures one or more characters that are NOT closing braces ([^}]+), and
    // finally matches the closing brace }.
    val regex = "\\{([^}]+)\\}"
    val matcher = Pattern.compile(regex).matcher(this)

    while (matcher.find()) {
        // group(0) would be the full match (e.g., "{counterName}")
        // group(1) is the content inside the braces (e.g., "counterName")
        matcher.group(1)?.let { counterName ->
            counterReferences.add(counterName)
        }
    }

    return counterReferences
}

fun String.replaceCounterReferences(counterToValueMap: Map<String, Int>): String {
    var result = this
    counterToValueMap.entries.forEach { (counterName, counterValue) ->
        result = result.replace(
            oldValue = "{$counterName}",
            newValue = counterValue.toString()
        )
    }

    return result
}