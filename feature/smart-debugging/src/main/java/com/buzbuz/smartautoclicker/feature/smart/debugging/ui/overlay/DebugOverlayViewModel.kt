
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.debugging.getIsDebugViewEnabled
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report.formatConfidenceRate

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/** ViewModel for the debug features. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugModel @Inject constructor(
    @ApplicationContext context: Context,
    debuggingRepository: DebuggingRepository
) : ViewModel() {

    /** Debug configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getDebugConfigPreferences()

    /** Tells if the current detection is running in debug mode. */
    val isDebugging = debuggingRepository.isDebugging.map { debugging ->
        debugging && sharedPreferences.getIsDebugViewEnabled(context)
    }

    /** The info on the last positive detection. */
    val debugLastPositive: Flow<LastPositiveDebugInfo> = debuggingRepository.lastPositiveInfo
        .transformLatest { debugInfo ->
            if (debugInfo == null) {
                emit(LastPositiveDebugInfo())
                return@transformLatest
            }

            emit(
                LastPositiveDebugInfo(
                    debugInfo.event.name,
                    debugInfo.condition.name,
                    debugInfo.confidenceRate.formatConfidenceRate(),
                )
            )
            delay(POSITIVE_VALUE_DISPLAY_TIMEOUT_MS)
            emit(LastPositiveDebugInfo())
        }
}

/**
 * Info on the last positive detection.
 * @param eventName name of the event
 * @param conditionName the name of the condition detected.
 * @param confidenceRateText the text to display for the confidence rate
 */
data class LastPositiveDebugInfo(
    val eventName: String = "",
    val conditionName: String = "",
    val confidenceRateText: String = "",
)

/** Delay before removing the last positive result display in debug. */
private const val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500L