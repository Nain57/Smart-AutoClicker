/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Returns a flow containing the results of applying the given transform function to each value of the original flow. */
inline fun <T, R> Flow<List<T>>.mapList(crossinline transform: suspend (value: T) -> R): Flow<List<R>> =
    map { list ->
        list.map { mapValue ->
            transform.invoke(mapValue)
        }
    }