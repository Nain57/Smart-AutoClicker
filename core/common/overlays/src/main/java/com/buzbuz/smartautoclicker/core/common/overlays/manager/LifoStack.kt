
package com.buzbuz.smartautoclicker.core.common.overlays.manager

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