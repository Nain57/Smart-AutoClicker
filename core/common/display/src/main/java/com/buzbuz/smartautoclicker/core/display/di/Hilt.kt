
package com.buzbuz.smartautoclicker.core.display.di

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.recorder.DisplayRecorder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@EntryPoint
@InstallIn(SingletonComponent::class)
interface DisplayEntryPoint {

    fun displayMetrics(): DisplayConfigManager
    fun displayRecorder(): DisplayRecorder
}