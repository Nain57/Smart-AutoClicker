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
package com.buzbuz.smartautoclicker.feature.scenario.config.data.base

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 *
 */
internal abstract class Editor<Reference, Value> {

    /**  */
    protected val reference: MutableStateFlow<Reference?> = MutableStateFlow(null)

    /** */
    protected val _editedValue: MutableStateFlow<Value?> = MutableStateFlow(null)
    /** */
    val editedValue: StateFlow<Value?> = _editedValue

    /** Start editing the configured item list. */
    fun startEdition(reference: Reference) {
        this.reference.value = reference
        _editedValue.value = getValueFromReference(reference)

        onEditionStarted(reference)
    }

    fun updateEditedValue(value: Value) {
        _editedValue.value = value
    }

    /** Finish the list edition and returns the last value. */
    fun finishEdition(): Reference {
        val result = onEditionFinished()

        _editedValue.value = null
        reference.value = null

        return result
    }

    internal fun getEditedValueOrThrow(): Value =
        editedValue.value ?: throw IllegalStateException("There is no edited value !")

    /** */
    protected fun getReferenceOrThrow(): Reference =
        reference.value ?: throw IllegalStateException("There is no reference !")

    /** */
    protected abstract fun getValueFromReference(reference: Reference): Value
    /** */
    protected open fun onEditionStarted(reference: Reference) { }
    /** */
    protected abstract fun onEditionFinished(): Reference
}