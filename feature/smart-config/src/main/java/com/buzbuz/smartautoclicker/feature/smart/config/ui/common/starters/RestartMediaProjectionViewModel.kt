
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class RestartMediaProjectionViewModel @Inject constructor(
    @Dispatcher(HiltCoroutineDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val detectionRepository: DetectionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun restartScreenRecord(resultCode: Int, data: Intent) {
        viewModelScope.launch(ioDispatcher) {
            detectionRepository.startScreenRecord(resultCode, data)
        }
    }

    fun isEntireScreenCaptureForced(): Boolean =
        settingsRepository.isEntireScreenCaptureForced()
}