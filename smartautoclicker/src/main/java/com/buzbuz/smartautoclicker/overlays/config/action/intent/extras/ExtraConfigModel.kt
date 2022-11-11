/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.overlays.config.action.intent.extras

import android.app.Application
import android.text.InputFilter
import android.text.InputType

import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.baseui.NumberInputFilter
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.overlays.base.dialog.DialogChoice
import com.buzbuz.smartautoclicker.overlays.base.utils.getDisplayNameRes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

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
                null -> ExtraValueInputState.NoTypeSelected
                is Boolean -> ExtraValueInputState.BooleanInputTypeSelected(
                    value::class.getDisplayNameRes(),
                    value,
                )
                else -> {
                    val inputInfo = getInputInfo(value)
                    ExtraValueInputState.TextInputTypeSelected(
                        value::class.getDisplayNameRes(),
                        inputInfo.second,
                        inputInfo.first,
                        value.toString(),
                        value,
                    )
                }
            }
        }
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
            configuredExtra.value = extra.copy()
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
    fun setBooleanValue(value: Boolean) {
        viewModelScope.launch {
            val extraValue = configuredExtra.value?.value?: return@launch
            if (extraValue is Boolean) {
                configuredExtra.value = configuredExtra.value?.copy(value = value)
            }
        }
    }

    /**
     * Set the type of the extra.
     * @param type the new type.
     *
     * @throws IllegalArgumentException if the type is not supported.
     */
    fun setType(type: ExtraTypeChoice) {
        viewModelScope.launch {
            val oldValue = configuredExtra.value ?: return@launch

            configuredExtra.value = when (type) {
                ExtraTypeChoice.ByteChoice -> oldValue.copy<Byte>(value = 0)
                ExtraTypeChoice.BooleanChoice -> oldValue.copy(value = false)
                ExtraTypeChoice.CharChoice -> oldValue.copy(value = 'a')
                ExtraTypeChoice.DoubleChoice -> oldValue.copy(value = 0.0)
                ExtraTypeChoice.IntChoice -> oldValue.copy(value = 0)
                ExtraTypeChoice.FloatChoice -> oldValue.copy(value = 0f)
                ExtraTypeChoice.ShortChoice -> oldValue.copy<Short>(value = 0)
                ExtraTypeChoice.StringChoice -> oldValue.copy(value = "")
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

    /** There is no types selected, no input views displayed. */
    object NoTypeSelected: ExtraValueInputState()

    sealed class TypeSelected : ExtraValueInputState() {
        /** The name of the selected type. */
        abstract val typeText: Int
    }

    /**
     * Selected type requires a text input with the IME.
     *
     * @param inputType the flags to be applied to the edit text.
     * @param inputFilter the filter to be applied to the edit text.
     * @param valueStr the value to be displayed in the edit text.
     * @param value the raw current value.
     */
    data class TextInputTypeSelected(
        @StringRes override val typeText: Int,
        val inputType: Int,
        val inputFilter: InputFilter?,
        val valueStr: String,
        val value: Any,
    ): TypeSelected()

    /**
     * Selected type requires a selection between two parameters.
     *
     * @param value value of the extra
     */
    data class BooleanInputTypeSelected(
        @StringRes override val typeText: Int,
        val value: Boolean,
    ): TypeSelected()
}

/** Choices for the extra type selection dialog. */
sealed class ExtraTypeChoice(title: Int): DialogChoice(title) {
    object BooleanChoice : ExtraTypeChoice(Boolean::class.getDisplayNameRes())
    object ByteChoice : ExtraTypeChoice(Byte::class.getDisplayNameRes())
    object CharChoice : ExtraTypeChoice(Char::class.getDisplayNameRes())
    object DoubleChoice : ExtraTypeChoice(Double::class.getDisplayNameRes())
    object FloatChoice : ExtraTypeChoice(Float::class.getDisplayNameRes())
    object IntChoice : ExtraTypeChoice(Int::class.getDisplayNameRes())
    object ShortChoice : ExtraTypeChoice(Short::class.getDisplayNameRes())
    object StringChoice: ExtraTypeChoice(String::class.getDisplayNameRes())

    companion object {
        fun getAllChoices() = listOf(
            BooleanChoice, ByteChoice, CharChoice, DoubleChoice, FloatChoice, IntChoice,
            ShortChoice, StringChoice
        )
    }
}