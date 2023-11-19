/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.utils.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal open class LifoStack<E> {

    private val backingArray: ArrayDeque<E> = ArrayDeque(emptyList())

    private val _top: MutableStateFlow<E?> = MutableStateFlow(null)
    val topFlow: StateFlow<E?> = _top

    val top: E?
        get() = if (isNotEmpty()) peek() else null

    val bottom: E?
        get() = if (isNotEmpty()) backingArray.first() else null

    val size: Int
        get() = backingArray.size

    fun isEmpty(): Boolean = backingArray.isEmpty()
    fun isNotEmpty(): Boolean = backingArray.isNotEmpty()

    fun peek(): E = backingArray.last()

    fun pop(): E {
        val poppedValue = backingArray.removeLast()
        _top.value = if (isNotEmpty()) backingArray.last() else null
        return poppedValue
    }

    open fun push(element: E) {
        backingArray.addLast(element)
        _top.value = element
    }

    fun contains(element: E): Boolean = backingArray.contains(element)

    fun indexOf(element: E): Int = backingArray.indexOf(element)

    inline fun forEach(action: (E) -> Unit): Unit =
        backingArray.forEach(action)

    inline fun forEachReversed(action: (E) -> Unit) {
        for (i in backingArray.lastIndex downTo 0) {
            action(backingArray[i])
        }
    }

    fun clear() {
        backingArray.clear()
        _top.value = null
    }

    fun toList(): List<E> = backingArray.toList()
}