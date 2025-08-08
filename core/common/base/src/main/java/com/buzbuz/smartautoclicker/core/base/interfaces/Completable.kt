
package com.buzbuz.smartautoclicker.core.base.interfaces

interface Completable {
    fun isComplete(): Boolean
}

fun List<Completable>.areComplete(): Boolean {
    forEach { completable ->
        if (!completable.isComplete()) return false
    }
    return true
}
