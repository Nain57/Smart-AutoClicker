
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.flags

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.android.intent.AndroidIntentApi
import com.buzbuz.smartautoclicker.core.android.intent.getBroadcastIntentFlags
import com.buzbuz.smartautoclicker.core.android.intent.getStartActivityIntentFlags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class FlagsSelectionViewModel @Inject constructor() : ViewModel() {

    private val isStartActivitiesFlags: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val selectedFlags: MutableStateFlow<Int> = MutableStateFlow(0)

    private val allAndroidFlags: Flow<List<AndroidIntentApi<Int>>> = isStartActivitiesFlags.mapNotNull { isStartActivity ->
        isStartActivity ?: return@mapNotNull null

        (if (isStartActivity == true) getStartActivityIntentFlags() else getBroadcastIntentFlags())
            .sortedBy { flag -> flag.displayName }
    }

    val flagsItems: Flow<List<ItemFlag>> =
        combine(allAndroidFlags, selectedFlags) { allFlags, selection ->
            allFlags.map { androidFlag ->
                ItemFlag(
                    flag = androidFlag,
                    isSelected = (selection and androidFlag.value) != 0,
                )
            }
        }

    fun getSelectedFlags(): Int =
        selectedFlags.value

    fun setSelectedFlags(flags: Int, startActivityFlags: Boolean) {
        isStartActivitiesFlags.value = startActivityFlags
        selectedFlags.value = flags
    }

    fun setFlagState(flag: Int, isSelected: Boolean) {
        selectedFlags.value =
            if (isSelected) selectedFlags.value or flag
            else selectedFlags.value and flag.inv()
    }
}

data class ItemFlag(
    val flag: AndroidIntentApi<Int>,
    val isSelected: Boolean,
)