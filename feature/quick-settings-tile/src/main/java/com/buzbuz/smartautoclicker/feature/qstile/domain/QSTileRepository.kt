
package com.buzbuz.smartautoclicker.feature.qstile.domain

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.feature.qstile.R
import com.buzbuz.smartautoclicker.feature.qstile.data.QSTileScenarioInfo
import com.buzbuz.smartautoclicker.feature.qstile.data.QsTileConfigDataSource
import com.buzbuz.smartautoclicker.feature.qstile.ui.QSTileService

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class QSTileRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
    private val smartEngine: DetectionRepository,
    private val qsTileConfigDataSource: QsTileConfigDataSource,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    private var qsTileActionHandler: QSTileActionHandler? = null

    private val tileDisplayInfo: Flow<QSTileDisplayInfo> = qsTileConfigDataSource.getQSTileScenarioInfo()
        .flatMapLatest { scenarioInfo ->
            scenarioInfo ?: return@flatMapLatest flowOf(context.getTileDisplayInfo(false, null, null, null))

                combine(smartRepository.getScenarioFlow(scenarioInfo.id), smartEngine.scenarioId) { scenario, runningId ->
                    context.getTileDisplayInfo(
                        isSmart = true,
                        runningId = runningId?.databaseId,
                        scenarioId = scenario?.id?.databaseId,
                        scenarioName = scenario?.name,
                    )
                }
        }

    internal val qsTileDisplayInfo: StateFlow<QSTileDisplayInfo?> = tileDisplayInfo
        .distinctUntilChanged()
        .stateIn(coroutineScopeIo, SharingStarted.Eagerly, null)

    init {
        qsTileDisplayInfo
            .onEach { QSTileService.requestTileUpdate(context) }
            .launchIn(coroutineScopeIo)
    }

    fun setTileScenario(scenarioId: Long, isSmart: Boolean) {
        coroutineScopeIo.launch {
            qsTileConfigDataSource.putQSTileScenarioInfo(QSTileScenarioInfo(scenarioId, isSmart))
        }
    }

    fun setTileActionHandler(actionHandler: QSTileActionHandler) {
        qsTileActionHandler = actionHandler
    }

    internal fun getLastScenarioDetails(): Pair<Long?, Boolean?> =
        qsTileDisplayInfo.value?.scenarioId to qsTileDisplayInfo.value?.isSmart

    internal fun isAccessibilityServiceStarted(): Boolean =
        qsTileActionHandler?.isRunning() ?: false

    internal fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) =
        qsTileActionHandler?.startSmartScenario(resultCode, data, scenario)

    internal fun stopScenarios() =
        qsTileActionHandler?.stop()

    private fun Context.getTileDisplayInfo(isSmart: Boolean, runningId: Long?, scenarioId: Long?, scenarioName: String?): QSTileDisplayInfo {
        val state = when {
            scenarioId == null || scenarioName == null -> Tile.STATE_UNAVAILABLE
            runningId == null -> Tile.STATE_INACTIVE
            scenarioId == runningId -> Tile.STATE_ACTIVE
            else -> Tile.STATE_UNAVAILABLE
        }

        return QSTileDisplayInfo(
            tileState = state,
            tileTitle = getString(
                when (state) {
                    Tile.STATE_INACTIVE -> R.string.tile_label_start_scenario
                    Tile.STATE_ACTIVE -> R.string.tile_label_stop_scenario
                    else -> R.string.tile_label_start_scenario
                }
            ),
            tileSubTitle =
                if (state == Tile.STATE_UNAVAILABLE) getString(R.string.tile_subtext_unavailable)
                else scenarioName,
            scenarioId = scenarioId,
            isSmart = isSmart,
        )
    }
}

