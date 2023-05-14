/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.action.intent.extras

import android.app.Application
import android.text.InputFilter
import android.text.InputType

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.baseui.utils.NumberInputFilter
import com.buzbuz.smartautoclicker.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.overlays.base.utils.getDisplayNameRes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * View model for the [ExtraConfigDialog].
 *
 * @param application the Android application.
 */
class ExtraConfigModel(application: Application) : AndroidViewModel(application) {

    /** The extra currently configured. */
    private val configuredExtra = MutableStateFlow<IntentExtra<out Any>?>(null)

    /** The key for the extra. */
    val key: Flow<String?> = configuredExtra
        .map { it?.key }
        .take(1)
    /** The state for the input views. Changes with the value type. */
    val valueInputState: Flow<ExtraValueInputState> = configuredExtra
        .map {
            when (val value = it?.value) {
                null,
                is Boolean -> ExtraValueInputState.BooleanInputTypeSelected(
                    BOOLEAN_ITEM,
                    if (value == true) BOOLEAN_ITEM_TRUE else BOOLEAN_ITEM_FALSE,
                )
                else -> {
                    val inputInfo = getInputInfo(value)
                    ExtraValueInputState.TextInputTypeSelected(
                        value::class.getTypeItem(),
                        inputInfo.second,
                        inputInfo.first,
                        value.toString(),
                        value,
                    )
                }
            }
        }

    /** Choices for the extra type selection dialog. */
    val extraTypeDropdownItems = listOf(
        BOOLEAN_ITEM, BYTE_ITEM, CHAR_ITEM, DOUBLE_ITEM, FLOAT_ITEM, INT_ITEM, SHORT_ITEM, STRING_ITEM,
    )
    /** Items for the boolean value dropdown field. */
    val booleanItems = listOf(BOOLEAN_ITEM_TRUE, BOOLEAN_ITEM_FALSE)

    /** Tells if the action name is valid or not. */
    val keyError: Flow<Boolean> = configuredExtra.map { it?.key?.isEmpty() ?: true }
    /** Tells if the action name is valid or not. */
    val valueError: Flow<Boolean> = configuredExtra.map { it?.value == null }

    /** Tells if this extra if valid for save or not. */
    val isExtraValid: Flow<Boolean> = configuredExtra
        .map { extra ->
            extra != null && !extra.key.isNullOrEmpty() && extra.value != null
        }

    /**
     * Set the extra configured by this model.
     * @param extra the extra to be configured.
     */
    fun setConfigExtra(extra: IntentExtra<out Any>) {
        viewModelScope.launch {
            configuredExtra.value =
                if (extra.value == null) extra.copy(value = false)
                else extra.copy()
        }
    }

    /**
     * Set the key of the extra.
     * @param key the new extra key.
     */
    fun setKey(key: String) {
        viewModelScope.launch {
            configuredExtra.value = configuredExtra.value?.copy(key = key)
        }
    }

    /**
     * Set the value of the extra.
     * @param value the new extra value.
     */
    fun setValue(value: String) {
        viewModelScope.launch {
            val extraValue = configuredExtra.value?: return@launch
            configuredExtra.value = extraValue.copyFromString(value)
        }
    }

    /** Set the value to true or false. Should be called for a Boolean extra only. */
    fun setBooleanValue(value: DropdownItem) {
        viewModelScope.launch {
            val extraValue = when (value) {
                BOOLEAN_ITEM_TRUE -> true
                BOOLEAN_ITEM_FALSE -> false
                else -> return@launch
            }

            configuredExtra.value = configuredExtra.value?.copy(value = extraValue)
        }
    }

    /**
     * Set the type of the extra.
     * @param type the new type.
     */
    fun setType(type: DropdownItem) {
        viewModelScope.launch {
            val oldValue = configuredExtra.value ?: return@launch

            configuredExtra.value = when (type) {
                BYTE_ITEM -> oldValue.copy<Byte>(value = 0)
                BOOLEAN_ITEM -> oldValue.copy(value = false)
                CHAR_ITEM -> oldValue.copy(value = 'a')
                DOUBLE_ITEM -> oldValue.copy(value = 0.0)
                INT_ITEM -> oldValue.copy(value = 0)
                FLOAT_ITEM -> oldValue.copy(value = 0f)
                SHORT_ITEM -> oldValue.copy<Short>(value = 0)
                STRING_ITEM -> oldValue.copy(value = "")
                else -> throw IllegalArgumentException("Unsupported extra type $type")
            }
        }
    }

    /** @return the extra currently configured. */
    fun getConfiguredExtra(): IntentExtra<out Any> = configuredExtra.value!!

    /**
     * Get the configuration for the IME for a given type.
     * @param type the type to get the configuration of.
     *
     * @return a pair of InputFilter to InputType.
     */
    private fun getInputInfo(type: Any): Pair<InputFilter?, Int> =
        when (type) {
            is Byte -> NumberInputFilter(Byte::class) to
                    (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
            is Short -> NumberInputFilter(Short::class) to
                    (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
            is Int -> NumberInputFilter(Int::class) to
                    (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
            is Long -> NumberInputFilter(Long::class) to
                    (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)

            is Double -> NumberInputFilter(Double::class) to
                    (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            is Float -> NumberInputFilter(Float::class) to
                    (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL)

            is Char -> InputFilter.LengthFilter(1) to InputType.TYPE_CLASS_TEXT
            is String -> null to InputType.TYPE_CLASS_TEXT

            else -> throw IllegalArgumentException("Unsupported value type")
        }
}

/** Copy a IntentExtra and change its value but keep its type. */
private fun IntentExtra<out Any>.copyFromString(strValue: String): IntentExtra<out Any> = when (value) {
    is Byte -> copy(value = if (strValue.isEmpty() || strValue == "-") 0 else strValue.toByte())
    is Boolean -> copy(value = strValue.toBoolean())
    is Char -> copy(value = if (strValue.isEmpty()) ' ' else strValue.toCharArray()[0])
    is Double -> copy(value = if (strValue.isEmpty() || strValue == "-") 0.0 else strValue.toDouble())
    is Int -> copy(value = if (strValue.isEmpty() || strValue == "-") 0 else strValue.toInt())
    is Float -> copy(value = if (strValue.isEmpty() || strValue == "-") 0f else strValue.toFloat())
    is Short -> copy(value = if (strValue.isEmpty() || strValue == "-") 0 else strValue.toShort())
    is String -> copy(value = strValue)
    else -> throw IllegalArgumentException("Unsupported value type")
}

/** State of the extra value input views. */
sealed class ExtraValueInputState {

    /** The selected type. */
    abstract val typeItem: DropdownItem

    /**
     * Selected type requires a text input with the IME.
     *
     * @param inputType the flags to be applied to the edit text.
     * @param inputFilter the filter to be applied to the edit text.
     * @param valueStr the value to be displayed in the edit text.
     * @param value the raw current value.
     */
    data class TextInputTypeSelected(
        override val typeItem: DropdownItem,
        val inputType: Int,
        val inputFilter: InputFilter?,
        val valueStr: String,
        val value: Any,
    ): ExtraValueInputState()

    /**
     * Selected type requires a selection between two parameters.
     *
     * @param value value of the extra
     */
    data class BooleanInputTypeSelected(
        override val typeItem: DropdownItem,
        val value: DropdownItem,
    ): ExtraValueInputState()
}

/** Get the corresponding extra type dropdown item. */
private fun KClass<out Any>.getTypeItem() : DropdownItem = when (this) {
    Boolean::class -> BOOLEAN_ITEM
    Byte::class -> BYTE_ITEM
    Char::class -> CHAR_ITEM
    Double::class -> DOUBLE_ITEM
    Float::class -> FLOAT_ITEM
    Int::class -> INT_ITEM
    Short::class -> SHORT_ITEM
    String::class -> STRING_ITEM
    else -> throw IllegalArgumentException("Unsupported extra type")
}

// Items for the extra type dropdown menu
private val BOOLEAN_ITEM = DropdownItem(title = Boolean::class.getDisplayNameRes())
private val BYTE_ITEM = DropdownItem(title = Byte::class.getDisplayNameRes())
private val CHAR_ITEM = DropdownItem(title = Char::class.getDisplayNameRes())
private val DOUBLE_ITEM = DropdownItem(title = Double::class.getDisplayNameRes())
private val FLOAT_ITEM = DropdownItem(title = Float::class.getDisplayNameRes())
private val INT_ITEM = DropdownItem(title = Int::class.getDisplayNameRes())
private val SHORT_ITEM = DropdownItem(title = Short::class.getDisplayNameRes())
private val STRING_ITEM = DropdownItem(title = String::class.getDisplayNameRes())

private val BOOLEAN_ITEM_TRUE = DropdownItem(title = R.string.dropdown_item_title_extra_boolean_true)
private val BOOLEAN_ITEM_FALSE = DropdownItem(title = R.string.dropdown_item_title_extra_boolean_false)