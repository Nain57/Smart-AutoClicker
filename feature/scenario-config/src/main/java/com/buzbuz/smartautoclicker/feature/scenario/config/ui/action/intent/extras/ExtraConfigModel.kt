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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.extras

import android.app.Application
import android.text.InputFilter
import android.text.InputType

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.utils.NumberInputFilter
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getDisplayNameRes
import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlin.reflect.KClass

/**
 * View model for the [ExtraConfigDialog].
 *
 * @param application the Android application.
 */
@OptIn(FlowPreview::class)
class ExtraConfigModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The extra currently configured. */
    private val configuredExtra = editionRepository.editionState.editedIntentExtraState
        .mapNotNull { it.value }


    /** Tells if the user is currently editing a Intent extra. If that's not the case, dialog should be closed. */
    val isEditingExtra: Flow<Boolean> = editionRepository.isEditingIntentExtra
        .distinctUntilChanged()
        .debounce(1000)

    /** The key for the extra. */
    val key: Flow<String?> = configuredExtra
        .map { it.key }
        .take(1)
    /** The state for the input views. Changes with the value type. */
    val valueInputState: Flow<ExtraValueInputState> = configuredExtra
        .map {
            when (val value = it.value) {
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
    val keyError: Flow<Boolean> = configuredExtra.map { it.key?.isEmpty() ?: true }
    /** Tells if the action name is valid or not. */
    val valueError: Flow<Boolean> = configuredExtra.map { it.value == null }

    /** Tells if this extra if valid for save or not. */
    val isExtraValid: Flow<Boolean> = editionRepository.editionState.editedIntentExtraState
        .map { it.canBeSaved }

    /**
     * Set the key of the extra.
     * @param key the new extra key.
     */
    fun setKey(key: String) {
        editionRepository.editionState.getEditedIntentExtra()?.let { extra ->
            editionRepository.updateEditedIntentExtra(extra.copy(key = key))
        }
    }

    /**
     * Set the value of the extra.
     * @param value the new extra value.
     */
    fun setValue(value: String) {
        editionRepository.editionState.getEditedIntentExtra()?.let { extra ->
            editionRepository.updateEditedIntentExtra(extra.copyFromString(value))
        }
    }

    /** Set the value to true or false. Should be called for a Boolean extra only. */
    fun setBooleanValue(value: DropdownItem) {
        val extraValue = when (value) {
            BOOLEAN_ITEM_TRUE -> true
            BOOLEAN_ITEM_FALSE -> false
            else -> return
        }

        editionRepository.editionState.getEditedIntentExtra()?.let { extra ->
            editionRepository.updateEditedIntentExtra(extra.changeType(value = extraValue))
        }
    }

    /**
     * Set the type of the extra.
     * @param type the new type.
     */
    fun setType(type: DropdownItem) {
        editionRepository.editionState.getEditedIntentExtra()?.let { extra ->
            editionRepository.updateEditedIntentExtra(
                when (type) {
                    BYTE_ITEM -> extra.changeType<Byte>(value = 0)
                    BOOLEAN_ITEM -> extra.changeType(value = false)
                    CHAR_ITEM -> extra.changeType(value = 'a')
                    DOUBLE_ITEM -> extra.changeType(value = 0.0)
                    INT_ITEM -> extra.changeType(value = 0)
                    FLOAT_ITEM -> extra.changeType(value = 0f)
                    SHORT_ITEM -> extra.changeType<Short>(value = 0)
                    STRING_ITEM -> extra.changeType(value = "")
                    else -> throw IllegalArgumentException("Unsupported extra type $type")
                }
            )
        }
    }

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
    is Byte -> changeType(value = if (strValue.isEmpty() || strValue == "-") 0 else strValue.toByte())
    is Boolean -> changeType(value = strValue.toBoolean())
    is Char -> changeType(value = if (strValue.isEmpty()) ' ' else strValue.toCharArray()[0])
    is Double -> changeType(value = if (strValue.isEmpty() || strValue == "-") 0.0 else strValue.toDouble())
    is Int -> changeType(value = if (strValue.isEmpty() || strValue == "-") 0 else strValue.toInt())
    is Float -> changeType(value = if (strValue.isEmpty() || strValue == "-") 0f else strValue.toFloat())
    is Short -> changeType(value = if (strValue.isEmpty() || strValue == "-") 0 else strValue.toShort())
    is String -> changeType(value = strValue)
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