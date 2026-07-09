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
package com.buzbuz.smartautoclicker.feature.smart.config.di

import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlayComponent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu.MainMenuModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief.SmartActionsBriefViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.changecounter.ChangeCounterViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click.offset.ClickOffsetViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click.ClickViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.IntentViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.activities.ActivitySelectionModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.component.ComponentSelectionModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.extras.ExtraConfigModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.flags.FlagsSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification.NotificationViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.pause.PauseViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.settext.SetTextViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.swipe.SwipeViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.system.SystemActionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.EventTogglesViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.ToggleEventViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.intent.IntentActionsSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.CaptureViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.areaselector.ConditionAreaSelectorViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.ImageConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.brief.ScreenConditionsBriefViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.ColorConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.capture.ColorCaptureViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.number.NumberConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.ScreenConditionSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.ScreenConditionTypeSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.TextConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.required.RequiredAlphabetViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.selection.AlphabetSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.TriggerConditionListViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.broadcast.BroadcastReceivedConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.selection.TriggerConditionTypeSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter.CounterReachedConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.timer.TimerReachedConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.action.ActionCopyViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.condition.ConditionCopyViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.event.EventCopyViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.event.FixEventsCopyViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.config.CountersConfigViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation.CountersCreationViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.reference.CounterReferenceViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection.CounterSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.EventDialogViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu.debugging.LiveDebuggingViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.ScenarioDialogViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.config.ScenarioConfigViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.imageevents.ImageEventListViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.more.MoreViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.triggerevents.TriggerEventListViewModel

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

@EntryPoint
@InstallIn(OverlayComponent::class)
interface ScenarioConfigViewModelsEntryPoint {

    fun actionCopyViewModel(): ActionCopyViewModel
    fun actionTypeSelectionViewModel(): ActionTypeSelectionViewModel
    fun activitySelectionViewModel(): ActivitySelectionModel
    fun alphabetSelectionViewModel(): AlphabetSelectionViewModel
    fun broadcastReceivedConditionViewModel(): BroadcastReceivedConditionViewModel
    fun captureViewModel(): CaptureViewModel
    fun colorCaptureViewModel(): ColorCaptureViewModel
    fun colorConditionViewModel(): ColorConditionViewModel
    fun changeCounterViewModel(): ChangeCounterViewModel
    fun clickOffsetViewModel(): ClickOffsetViewModel
    fun clickViewModel(): ClickViewModel
    fun componentSelectionViewModel(): ComponentSelectionModel
    fun conditionCopyViewModel(): ConditionCopyViewModel
    fun counterCreationViewModel(): CountersCreationViewModel
    fun counterReferenceViewModel(): CounterReferenceViewModel
    fun counterSelectionViewModel(): CounterSelectionViewModel
    fun countersViewModel(): CountersConfigViewModel
    fun counterReachedConditionViewModel(): CounterReachedConditionViewModel
    fun eventCopyModel(): EventCopyViewModel
    fun eventDialogViewModel(): EventDialogViewModel
    fun eventTogglesViewModel(): EventTogglesViewModel
    fun extraConfigViewModel(): ExtraConfigModel
    fun fixEventChildrenCopyViewModel(): FixEventChildrenCopyViewModel
    fun fixEventsCopyViewModel(): FixEventsCopyViewModel
    fun flagsSelectionViewModel(): FlagsSelectionViewModel
    fun imageConditionAreaSelectorViewModel(): ConditionAreaSelectorViewModel
    fun screenConditionsBriefViewModel(): ScreenConditionsBriefViewModel
    fun imageConditionViewModel(): ImageConditionViewModel
    fun imageEventListViewModel(): ImageEventListViewModel
    fun intentActionsSelectionViewModel(): IntentActionsSelectionViewModel
    fun intentViewModel(): IntentViewModel
    fun liveDebuggingViewModel(): LiveDebuggingViewModel
    fun mainMenuViewModel(): MainMenuModel
    fun moreViewModel(): MoreViewModel
    fun numberConditionViewModel(): NumberConditionViewModel
    fun notificationViewModel(): NotificationViewModel
    fun pauseViewModel(): PauseViewModel
    fun requiredAlphabetViewModel(): RequiredAlphabetViewModel
    fun scenarioConfigViewModel(): ScenarioConfigViewModel
    fun scenarioDialogViewModel(): ScenarioDialogViewModel
    fun screenConditionSelectionViewModel(): ScreenConditionSelectionViewModel
    fun screenConditionTypeSelectionViewModel(): ScreenConditionTypeSelectionViewModel
    fun setTextViewModel(): SetTextViewModel
    fun smartActionsBriefViewModel(): SmartActionsBriefViewModel
    fun swipeViewModel(): SwipeViewModel
    fun systemActionViewModel(): SystemActionViewModel
    fun textConditionViewModel(): TextConditionViewModel
    fun timerReachedConditionViewModel(): TimerReachedConditionViewModel
    fun toggleEventViewModel(): ToggleEventViewModel
    fun triggerConditionTypeSelectionViewModel(): TriggerConditionTypeSelectionViewModel
    fun triggerConditionsViewModel(): TriggerConditionListViewModel
    fun triggerEventListViewModel(): TriggerEventListViewModel
}