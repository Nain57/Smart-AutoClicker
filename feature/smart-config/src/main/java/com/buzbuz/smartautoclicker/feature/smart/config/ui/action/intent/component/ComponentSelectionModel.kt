
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.component

import android.content.Context

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.android.application.AndroidApplicationInfo
import com.buzbuz.smartautoclicker.core.android.application.getAllAndroidApplicationsInfo

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/** View model for the [ComponentSelectionDialog]. */
class ComponentSelectionModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    /** Retrieves the list of activities visible on the Android launcher. */
    val activities: Flow<List<AndroidApplicationInfo>> = flow {
        emit(
            context.packageManager.getAllAndroidApplicationsInfo()
                .sortedBy { it.name.lowercase() })
    }.flowOn(Dispatchers.IO)

}