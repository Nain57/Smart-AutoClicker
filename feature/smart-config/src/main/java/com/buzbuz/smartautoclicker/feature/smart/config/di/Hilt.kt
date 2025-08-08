
package com.buzbuz.smartautoclicker.feature.smart.config.di

import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlayComponent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.MainMenuModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief.SmartActionsBriefViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.changecounter.ChangeCounterViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click.ClickOffsetViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click.ClickViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.copy.ActionCopyModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.IntentViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.activities.ActivitySelectionModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.component.ComponentSelectionModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.extras.ExtraConfigModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.flags.FlagsSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification.NotificationViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.pause.PauseViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.swipe.SwipeViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.EventTogglesViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.ToggleEventViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.counter.CounterNameSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.intent.IntentActionsSelectionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy.ConditionCopyModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.CaptureViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.ImageConditionAreaSelectorViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.ImageConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.brief.ImageConditionsBriefViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.TriggerConditionListViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.broadcast.BroadcastReceivedConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter.CounterReachedConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.timer.TimerReachedConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.EventDialogViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.copy.EventCopyModel
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

    fun actionCopyViewModel(): ActionCopyModel
    fun actionTypeSelectionViewModel(): ActionTypeSelectionViewModel
    fun activitySelectionViewModel(): ActivitySelectionModel
    fun broadcastReceivedConditionViewModel(): BroadcastReceivedConditionViewModel
    fun captureViewModel(): CaptureViewModel
    fun changeCounterViewModel(): ChangeCounterViewModel
    fun clickOffsetViewModel(): ClickOffsetViewModel
    fun clickViewModel(): ClickViewModel
    fun componentSelectionViewModel(): ComponentSelectionModel
    fun conditionCopyViewModel(): ConditionCopyModel
    fun counterNameSelectionViewModel(): CounterNameSelectionViewModel
    fun counterReachedConditionViewModel(): CounterReachedConditionViewModel
    fun eventCopyModel(): EventCopyModel
    fun eventDialogViewModel(): EventDialogViewModel
    fun eventTogglesViewModel(): EventTogglesViewModel
    fun extraConfigViewModel(): ExtraConfigModel
    fun flagsSelectionViewModel(): FlagsSelectionViewModel
    fun imageConditionAreaSelectorViewModel(): ImageConditionAreaSelectorViewModel
    fun imageConditionsBriefViewModel(): ImageConditionsBriefViewModel
    fun imageConditionViewModel(): ImageConditionViewModel
    fun imageEventListViewModel(): ImageEventListViewModel
    fun intentActionsSelectionViewModel(): IntentActionsSelectionViewModel
    fun intentViewModel(): IntentViewModel
    fun mainMenuViewModel(): MainMenuModel
    fun moreViewModel(): MoreViewModel
    fun notificationViewModel(): NotificationViewModel
    fun pauseViewModel(): PauseViewModel
    fun scenarioConfigViewModel(): ScenarioConfigViewModel
    fun scenarioDialogViewModel(): ScenarioDialogViewModel
    fun smartActionsBriefViewModel(): SmartActionsBriefViewModel
    fun swipeViewModel(): SwipeViewModel
    fun timerReachedConditionViewModel(): TimerReachedConditionViewModel
    fun toggleEventViewModel(): ToggleEventViewModel
    fun triggerConditionsViewModel(): TriggerConditionListViewModel
    fun triggerEventListViewModel(): TriggerEventListViewModel
}