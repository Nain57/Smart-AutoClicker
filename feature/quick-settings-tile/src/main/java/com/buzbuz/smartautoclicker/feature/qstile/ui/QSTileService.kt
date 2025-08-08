
package com.buzbuz.smartautoclicker.feature.qstile.ui

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.base.extensions.startActivityAndCollapseCompat
import com.buzbuz.smartautoclicker.feature.qstile.domain.QSTileDisplayInfo
import com.buzbuz.smartautoclicker.feature.qstile.domain.QSTileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QSTileService : TileService() {

    internal companion object {

        /**
         * Depending on the device, the tile service might not be started and thus, can't collect the tile info flow.
         * To fix this, call this static method to request an update of the tile to the system, which will start
         * temporarily the service, call [onStartListening] and update the tile before being destroyed right away.
         */
        fun requestTileUpdate(context: Context) {
            Log.d(TAG, "requestTileUpdate")

            try {
                requestListeningState(context, ComponentName(context, QSTileService::class.java))
            } catch (iaEx: IllegalArgumentException) {
                Log.e(TAG, "Can't request tile update, system is denying it")
            }
        }
    }

    @Inject internal lateinit var qsTileRepository: QSTileRepository
    @Inject @Dispatcher(HiltCoroutineDispatchers.Main) internal lateinit var mainDispatcher: CoroutineDispatcher

    private var mainCoroutineScope: CoroutineScope? = null
    private var collectJob: Job? = null

    private var isTileAdded: Boolean = false
    private var isListening: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        mainCoroutineScope = CoroutineScope(SupervisorJob() + mainDispatcher)
        collectJob?.cancel()

        // On some devices, the service can be long lived (i.e following the normal Android implementation),
        // In that case, we need to collect the flow to receive updates while in this state
        collectJob = mainCoroutineScope?.launch {
            qsTileRepository.qsTileDisplayInfo.collect(::updateTile)
        }
    }

    // Called when the user adds your tile.
    override fun onTileAdded() {
        Log.d(TAG, "onTileAdded")
        isTileAdded = true
    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        Log.d(TAG, "onStartListening")
        isListening = true
        updateTile(qsTileRepository.qsTileDisplayInfo.value)
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        Log.d(TAG, "onStopListening")
        isListening = false
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        Log.d(TAG, "onClick")
        val (scenarioId, isSmart) = qsTileRepository.getLastScenarioDetails()
        if (scenarioId == null || isSmart == null) return

        when (qsTileRepository.qsTileDisplayInfo.value?.tileState) {
            Tile.STATE_ACTIVE ->
                qsTileRepository.stopScenarios()
            Tile.STATE_INACTIVE ->
                startActivityAndCollapseCompat(QSTileLauncherActivity.getStartIntent(this, scenarioId, isSmart))
        }
    }

    // Called when the user removes your tile.
    override fun onTileRemoved() {
        Log.d(TAG, "onTileRemoved")
        isTileAdded = false
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        collectJob?.cancel()
        collectJob = null
        mainCoroutineScope?.cancel()

        super.onDestroy()
    }

    private fun updateTile(tileDisplayInfo: QSTileDisplayInfo?) {
        if (!isListening || tileDisplayInfo == null) return

        Log.d(TAG, "updateTile with $tileDisplayInfo")
        qsTile?.apply {
            state = tileDisplayInfo.tileState
            label = tileDisplayInfo.tileTitle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = tileDisplayInfo.tileSubTitle
            }

            updateTile()
        }
    }
}

private const val TAG = "QSTileService"