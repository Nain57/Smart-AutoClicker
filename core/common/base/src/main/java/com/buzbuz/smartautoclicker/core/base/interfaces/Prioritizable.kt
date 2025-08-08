
package com.buzbuz.smartautoclicker.core.base.interfaces

interface Prioritizable {
    var priority: Int
}

fun <T : Prioritizable> Collection<T>.sortedByPriority(): Collection<T> =
    sortedBy { it.priority }

fun Collection<Prioritizable>.normalizePriorities() {
    forEachIndexed { index, item -> item.priority = index }
}