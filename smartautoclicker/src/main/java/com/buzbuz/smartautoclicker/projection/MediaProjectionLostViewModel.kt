/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.projection

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.SmartAutoClickerService
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.localservice.ILocalService

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaProjectionLostViewModel @Inject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    /** Callback upon the availability of the [SmartAutoClickerService]. */
    private val serviceConnection: (ILocalService?) -> Unit = { localService ->
        clickerService = localService
    }

    /**
     * Reference on the [SmartAutoClickerService].
     * Will be not null only if the Accessibility Service is enabled.
     */
    private var clickerService: ILocalService? = null

    init {
        SmartAutoClickerService.getLocalService(serviceConnection)
    }

    override fun onCleared() {
        SmartAutoClickerService.getLocalService(null)
        super.onCleared()
    }

    fun startSmartScenario(resultCode: Int, data: Intent) {
        val service = clickerService ?: let {
            Log.e(TAG, "Can't start smart scenario, Service is not started")
            return
        }

        viewModelScope.launch(ioDispatcher) {
            service.retryStartSmartScenario(resultCode, data)
        }
    }

    fun stopApp() {
        val service = clickerService ?: let {
            Log.e(TAG, "Can't stop, Service is not started")
            return
        }

        service.stop()
    }
}

private const val TAG = "MediaProjectionLostViewModel"