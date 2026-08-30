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
package com.buzbuz.smartautoclicker.feature.externallaunch.domain

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.scenariostate.ScenarioState
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.scenariostate.ScenarioStatePluginContract
import com.buzbuz.smartautoclicker.feature.externallaunch.qstile.data.QSTileScenarioInfo
import com.buzbuz.smartautoclicker.feature.externallaunch.qstile.data.QsTileConfigDataSource
import com.buzbuz.smartautoclicker.feature.externallaunch.qstile.domain.QSTileDisplayInfo
import com.buzbuz.smartautoclicker.feature.externallaunch.qstile.ui.QSTileService

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
class ExternalLaunchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val dumbRepository: DumbRepository,
    private val dumbEngine: DumbEngine,
    private val smartRepository: IRepository,
    private val smartProcessingRepository: SmartProcessingRepository,
    private val qsTileConfigDataSource: QsTileConfigDataSource,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    private var actionHandler: ExternalLaunchActionHandler? = null
    private var lastPublishedScenarioStatus: PublishedScenarioStatus? = null

    private val tileDisplayInfo: Flow<QSTileDisplayInfo> = qsTileConfigDataSource.getQSTileScenarioInfo()
        .flatMapLatest { scenarioInfo ->
            scenarioInfo ?: return@flatMapLatest flowOf(context.getTileDisplayInfo(false, null, null, null))

            if (scenarioInfo.isSmart) {
                combine(smartRepository.getScenarioFlow(scenarioInfo.id), smartProcessingRepository.scenarioId) { scenario, runningId ->
                    context.getTileDisplayInfo(
                        isSmart = true,
                        runningId = runningId?.databaseId,
                        scenarioId = scenario?.id?.databaseId,
                        scenarioName = scenario?.name,
                    )
                }
            } else {
                combine(dumbRepository.getDumbScenarioFlow(scenarioInfo.id), dumbEngine.dumbScenario) { scenario, runningScenario ->
                    context.getTileDisplayInfo(
                        isSmart = false,
                        runningId = runningScenario?.getDatabaseId(),
                        scenarioId = scenario?.id?.databaseId,
                        scenarioName = scenario?.name,
                    )
                }
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

    fun setActionHandler(actionHandler: ExternalLaunchActionHandler) {
        this.actionHandler = actionHandler
    }

    /** Ask every active Locale/Tasker host to query the current scenario condition again. */
    @Synchronized
    fun notifyScenarioStateChanged() {
        val scenarioId = getSmartScenarioId() ?: getDumbScenarioId()
        val status = PublishedScenarioStatus(
            scenarioId = scenarioId,
            state = ScenarioState.from(
                isScenarioOpen = scenarioId != null,
                isRunning = isScenarioRunning(),
                isOverlayHidden = isOverlayHidden(),
                isSettingsOpen = isScenarioConfigurationOpen(),
            ),
        )
        if (status == lastPublishedScenarioStatus) return
        Log.d(TAG, "Scenario status changed: $lastPublishedScenarioStatus -> $status; requesting requery")
        lastPublishedScenarioStatus = status

        context.sendBroadcast(
            LocalePluginContract.createRequestQueryIntent(
                ScenarioStatePluginContract.CONFIGURATION_ACTIVITY_CLASS_NAME,
            ),
        )
    }

    internal fun getLastScenarioDetails(): Pair<Long?, Boolean?> =
        qsTileDisplayInfo.value?.scenarioId to qsTileDisplayInfo.value?.isSmart

    internal fun isAccessibilityServiceStarted(): Boolean =
        actionHandler?.isRunning() ?: false

    internal fun isScenarioRunning(): Boolean =
        actionHandler?.isScenarioRunning() ?: false

    internal fun isOverlayVisible(): Boolean =
        actionHandler?.isOverlayVisible() ?: false

    internal fun isOverlayHidden(): Boolean =
        actionHandler?.isOverlayHidden() ?: false

    /** True while the user is editing the currently loaded scenario. */
    internal fun isScenarioConfigurationOpen(): Boolean =
        actionHandler?.isScenarioConfigurationOpen() ?: false

    internal fun isSmartScreenRecordActive(): Boolean =
        actionHandler?.isSmartScreenRecordActive() ?: false

    internal fun getSmartScenarioId(): Long? =
        actionHandler?.getSmartScenarioId()

    internal fun getDumbScenarioId(): Long? =
        actionHandler?.getDumbScenarioId()

    internal fun isDumbScenarioRunning(scenarioId: Long): Boolean =
        actionHandler?.isRunning() == true && actionHandler?.getDumbScenarioId() == scenarioId

    internal fun launchDumbScenario(scenario: DumbScenario) =
        actionHandler?.launchDumbScenario(scenario)

    internal fun launchSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) =
        actionHandler?.launchSmartScenario(resultCode, data, scenario)

    internal fun replaceDumbScenario(scenario: DumbScenario) =
        actionHandler?.replaceDumbScenario(scenario)

    internal fun replaceSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) =
        actionHandler?.replaceSmartScenario(resultCode, data, scenario)

    internal fun replaceSmartScenarioWithCurrentProjection(scenario: Scenario) =
        actionHandler?.replaceSmartScenarioWithCurrentProjection(scenario)

    internal fun runCurrentScenario() =
        actionHandler?.runCurrentScenario()

    internal fun stopScenarios() =
        actionHandler?.stop()

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
                    Tile.STATE_INACTIVE -> R.string.tile_label_launch_scenario
                    Tile.STATE_ACTIVE -> R.string.tile_label_stop_scenario
                    else -> R.string.tile_label_launch_scenario
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

private data class PublishedScenarioStatus(
    val scenarioId: Long?,
    val state: ScenarioState,
)

private const val TAG = "ExternalLaunchRepository"

