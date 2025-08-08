
package com.buzbuz.smartautoclicker.core.base.extensions

import kotlin.math.min

/** Returns a new list stripped from all element with index equals or over [toIndex]. */
fun <T> List<T>.trim(toIndex: Int): List<T> =
    subList(0, min(size, toIndex))