
package com.buzbuz.smartautoclicker.feature.dumb.config.di

import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlayComponent
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.DumbMainMenuModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.click.DumbClickViewModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy.DumbActionCopyModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.pause.DumbPauseViewModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.swipe.DumbSwipeViewModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.brief.DumbScenarioBriefViewModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.DumbScenarioViewModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.actionlist.DumbActionListViewModel
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.config.DumbScenarioConfigContentViewModel

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

@EntryPoint
@InstallIn(OverlayComponent::class)
interface DumbConfigViewModelsEntryPoint {
    fun dumbActionCopyModel(): DumbActionCopyModel
    fun dumbActionListViewModel(): DumbActionListViewModel
    fun dumbClickViewModel(): DumbClickViewModel
    fun dumbMainMenuModel(): DumbMainMenuModel
    fun dumbPauseViewModel(): DumbPauseViewModel
    fun dumbScenarioBriefViewModel(): DumbScenarioBriefViewModel
    fun dumbScenarioViewModel(): DumbScenarioViewModel
    fun dumbScenarioConfigContentViewModel(): DumbScenarioConfigContentViewModel
    fun dumbSwipeViewModel(): DumbSwipeViewModel
}