
package com.buzbuz.smartautoclicker.feature.smart.config.domain

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.identifier.IdentifierCreator
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.bitmaps.CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Click.PositionType
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.data.ScenarioEditor

import android.os.Build
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Axis
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Back
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideMethod
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Home
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.KeyEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.LongPress
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.OpenNotifications
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.OpenQuickSettings
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Recents
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Screenshot
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Scroll
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.ShowKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.TypeText

class EditedItemsBuilder internal constructor(
    private val repository: IRepository,
    private val bitmapRepository: BitmapRepository,
    private val editor: ScenarioEditor,
) {

    private val defaultValues = EditionDefaultValues(repository)
    private val eventsIdCreator = IdentifierCreator()
    private val conditionsIdCreator = IdentifierCreator()
    private val actionsIdCreator = IdentifierCreator()
    private val intentExtrasIdCreator = IdentifierCreator()
    private val eventTogglesIdCreator = IdentifierCreator()
    private val endConditionsIdCreator = IdentifierCreator()

    /**
     * Map of original condition list ids to copy condition ids.
     * Will contain data only when creating an event from another one.
     */
    private val eventCopyConditionIdMap =  mutableMapOf<Identifier, Identifier>()

    /** Keep track of new images created during the edition session. */
    private val _newImageConditionsPaths: MutableList<String> = mutableListOf()
    internal val newImageConditionsPaths: List<String> = _newImageConditionsPaths

    internal fun resetBuilder() {
        eventsIdCreator.resetIdCount()
        conditionsIdCreator.resetIdCount()
        actionsIdCreator.resetIdCount()
        intentExtrasIdCreator.resetIdCount()
        endConditionsIdCreator.resetIdCount()
        eventCopyConditionIdMap.clear()
        _newImageConditionsPaths.clear()
    }

    fun createNewImageEvent(context: Context): ImageEvent =
        ImageEvent(
            id = eventsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = defaultValues.eventName(context),
            conditionOperator = defaultValues.eventConditionOperator(),
            priority = getEditedImageEventsCountOrThrow(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
            keepDetecting = false,
        )

    fun createNewTriggerEvent(context: Context): TriggerEvent =
        TriggerEvent(
            id = eventsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = defaultValues.eventName(context),
            conditionOperator = defaultValues.eventConditionOperator(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
        )

    fun createNewImageEventFrom(from: ImageEvent, scenarioId: Identifier = getEditedScenarioIdOrThrow()): ImageEvent {
        val eventId = eventsIdCreator.generateNewIdentifier()

        return from.copy(
            id = eventId,
            scenarioId = scenarioId,
            name = "" + from.name,
            conditions = from.conditions.map { conditionOrig ->
                val conditionCopy = createNewImageConditionFrom(conditionOrig, eventId)
                eventCopyConditionIdMap[conditionOrig.id] = conditionCopy.id
                conditionCopy
            },
            actions = from.actions.map { createNewActionFrom(it, eventId) }
        ).also { eventCopyConditionIdMap.clear() }
    }

    fun createNewTriggerEventFrom(from: TriggerEvent, scenarioId: Identifier = getEditedScenarioIdOrThrow()): TriggerEvent {
        val eventId = eventsIdCreator.generateNewIdentifier()

        return from.copy(
            id = eventId,
            scenarioId = scenarioId,
            name = "" + from.name,
            conditions = from.conditions.map { conditionOrig ->
                val conditionCopy = createNewTriggerConditionFrom(conditionOrig, eventId)
                eventCopyConditionIdMap[conditionOrig.id] = conditionCopy.id
                conditionCopy
            },
            actions = from.actions.map { createNewActionFrom(it, eventId) }
        ).also { eventCopyConditionIdMap.clear() }
    }

    suspend fun createNewImageCondition(context: Context, area: Rect, bitmap: Bitmap): ImageCondition {
        val id = conditionsIdCreator.generateNewIdentifier()
        val newPath = bitmapRepository.saveImageConditionBitmap(
            bitmap = bitmap,
            prefix = CONDITION_FILE_PREFIX,
        )
        _newImageConditionsPaths.add(newPath)

        return ImageCondition(
            id = id,
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            area = area,
            threshold = defaultValues.conditionThreshold(context),
            detectionType = defaultValues.conditionDetectionType(),
            shouldBeDetected = defaultValues.conditionShouldBeDetected(),
            path = newPath,
            priority = 0,
        )
    }

    fun createNewImageConditionFrom(condition: ImageCondition, eventId: Identifier = getEditedEventIdOrThrow()): ImageCondition =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            path = "" + condition.path,
        )

    fun createNewOnBroadcastReceived(context: Context): TriggerCondition.OnBroadcastReceived =
        TriggerCondition.OnBroadcastReceived(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            intentAction = "",
        )

    fun createNewOnCounterReached(context: Context): TriggerCondition.OnCounterCountReached =
        TriggerCondition.OnCounterCountReached(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            counterName = "",
            comparisonOperation = defaultValues.counterComparisonOperation(),
            counterValue = CounterOperationValue.Number(0)
        )

    fun createNewOnTimerReached(context: Context): TriggerCondition.OnTimerReached =
        TriggerCondition.OnTimerReached(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.conditionName(context),
            durationMs = 0,
            restartWhenReached = false,
        )

    fun createNewTriggerConditionFrom(condition: TriggerCondition, eventId: Identifier = getEditedEventIdOrThrow()): TriggerCondition =
        when (condition) {
            is TriggerCondition.OnBroadcastReceived -> createNewOnBroadcastReceivedFrom(condition, eventId)
            is TriggerCondition.OnCounterCountReached -> createNewOnCounterReachedFrom(condition, eventId)
            is TriggerCondition.OnTimerReached -> createNewOnTimerReachedFrom(condition, eventId)
        }

    private fun createNewOnBroadcastReceivedFrom(condition: TriggerCondition.OnBroadcastReceived, eventId: Identifier) =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            intentAction = "" + condition.intentAction,
        )

    private fun createNewOnCounterReachedFrom(condition: TriggerCondition.OnCounterCountReached, eventId: Identifier) =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            counterName = "" + condition.counterName,
        )

    private fun createNewOnTimerReachedFrom(condition: TriggerCondition.OnTimerReached, eventId: Identifier) =
        condition.copy(
            id = conditionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
        )

    fun createNewClick(context: Context): Click =
        Click(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.clickName(context),
            pressDuration = defaultValues.clickPressDuration(context),
            positionType = defaultValues.clickPositionType(),
            priority = 0,
        )

    fun createNewSwipe(context: Context): Swipe =
        Swipe(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.swipeName(context),
            swipeDuration = defaultValues.swipeDuration(context),
            priority = 0,
        )

    fun createNewPause(context: Context): Pause =
        Pause(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.pauseName(context),
            pauseDuration = defaultValues.pauseDuration(context),
            priority = 0,
        )

    fun createNewIntent(context: Context): Intent =
        Intent(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.intentName(context),
            isBroadcast = false,
            isAdvanced = defaultValues.intentIsAdvanced(context),
            priority = 0,
        )

    fun createNewIntentExtra() : IntentExtra<Any> =
        IntentExtra(
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = getEditedActionIdOrThrow(),
            key = null,
            value = null,
        )

    fun createNewToggleEvent(context: Context): ToggleEvent =
        ToggleEvent(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.toggleEventName(context),
            toggleAll = false,
            toggleAllType = null,
            eventToggles = emptyList(),
            priority = 0,
        )

    fun createNewEventToggle(
        id: Identifier = eventTogglesIdCreator.generateNewIdentifier(),
        targetEventId: Identifier? = null,
        toggleType: ToggleEvent.ToggleType = defaultValues.eventToggleType(),
    ) = EventToggle(
            id = id,
            actionId = getEditedActionIdOrThrow(),
            targetEventId = targetEventId,
            toggleType = toggleType,
        )

    fun createNewChangeCounter(context: Context): ChangeCounter =
        ChangeCounter(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.changeCounterName(context),
            counterName = "",
            operation = ChangeCounter.OperationType.ADD,
            operationValue = CounterOperationValue.Number(0),
            priority = 0,
        )

    fun createNewNotification(context: Context): Notification =
        Notification(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = defaultValues.notificationName(context),
            channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
            messageType = Notification.MessageType.TEXT,
            messageText = "",
            messageCounterName = "",
            priority = 0,
        )

    fun createNewActionFrom(from: Action, eventId: Identifier = getEditedEventIdOrThrow()): Action = when (from) {
        is Click -> createNewClickFrom(from, eventId)
        is Swipe -> createNewSwipeFrom(from, eventId)
        is Pause -> createNewPauseFrom(from, eventId)
        is Intent -> createNewIntentFrom(from, eventId)
        is ToggleEvent -> createNewToggleEventFrom(from, eventId)
        is ChangeCounter -> createNewChangeCounterFrom(from, eventId)
        is Notification -> createNewNotificationFrom(from, eventId)

        is LongPress -> createNewLongPressFrom(from, eventId)
        is Scroll -> createNewScrollFrom(from, eventId)
        is Back -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name
        )
        is Home -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name
        )
        is Recents -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name
        )
        is OpenNotifications -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name
        )
        is OpenQuickSettings -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name
        )
        is Screenshot -> createNewScreenshotFrom(from, eventId)
        is HideKeyboard -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name
        )
        is ShowKeyboard -> createNewShowKeyboardFrom(from, eventId)
        is TypeText -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name, text = "" + from.text
        )
        is KeyEvent -> from.copy(
            id = actionsIdCreator.generateNewIdentifier(), eventId = eventId, name = "" + from.name,
            codes = from.codes?.toList() ?: emptyList(), intervalMs = from.intervalMs
        )
    }

    private fun createNewClickFrom(from: Click, eventId: Identifier): Click {
        val conditionId =
            if (from.positionType == PositionType.ON_DETECTED_CONDITION && from.clickOnConditionId != null)
                eventCopyConditionIdMap[from.clickOnConditionId]
            else null

        return from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
            clickOnConditionId = conditionId,
        )
    }

    private fun createNewSwipeFrom(from: Swipe, eventId: Identifier): Swipe =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewPauseFrom(from: Pause, eventId: Identifier): Pause =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewIntentFrom(from: Intent, eventId: Identifier): Intent {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            intentAction = "" + from.intentAction,
            componentName = from.componentName?.clone(),
            extras = from.extras?.map { extra -> createNewIntentExtraFrom(extra, eventId) }
        )
    }

    private fun createNewIntentExtraFrom(from: IntentExtra<out Any>, actionId: Identifier = getEditedActionIdOrThrow()): IntentExtra<out Any> =
        from.copy(
            id = intentExtrasIdCreator.generateNewIdentifier(),
            actionId = actionId,
            key = "" + from.key,
        )

    private fun createNewToggleEventFrom(from: ToggleEvent, eventId: Identifier): ToggleEvent {
        val actionId = actionsIdCreator.generateNewIdentifier()

        val eventsToggles = from.eventToggles.mapNotNull { eventToggle ->
            // Check if the current edited scenario contains the event modified by the child event toggle.
            // Filter if not
            if (eventToggle.targetEventId == eventId || isEventIdValidInEditedScenario(eventId)) {
                createEventToggleFrom(eventToggle, actionId)
            } else null
        }

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            eventToggles = eventsToggles,
        )
    }

    private fun createEventToggleFrom(from: EventToggle, actionId: Identifier = getEditedActionIdOrThrow()): EventToggle =
        from.copy(
            id = eventTogglesIdCreator.generateNewIdentifier(),
            actionId = actionId,
        )

    private fun createNewChangeCounterFrom(from: ChangeCounter, eventId: Identifier): ChangeCounter {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            counterName = "" + from.counterName,
        )
    }

    private fun createNewNotificationFrom(from: Notification, eventId: Identifier): Notification {
        val actionId = actionsIdCreator.generateNewIdentifier()

        return from.copy(
            id = actionId,
            eventId = eventId,
            name = "" + from.name,
            messageText = "" + from.messageText,
            messageCounterName = "" + from.messageCounterName,
        )
    }

    fun createNewLongPress(context: Context): LongPress =
        LongPress(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Long press",
            holdDuration = 600L, // human-like default
            positionType = defaultValues.clickPositionType(), // reuse Click targeting semantics
            priority = 0,
        )

    fun createNewScroll(context: Context): Scroll =
        Scroll(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Scroll",
            axis = Axis.DOWN,
            distancePercent = 0.60f,
            duration = 350L,
            stutter = true,
            priority = 0,
        )

    fun createNewBack(context: Context): Back =
        Back(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Back",
            priority = 0,
        )

    fun createNewHome(context: Context): Home =
        Home(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Home",
            priority = 0,
        )

    fun createNewRecents(context: Context): Recents =
        Recents(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Recents",
            priority = 0,
        )

    fun createNewOpenNotifications(context: Context): OpenNotifications =
        OpenNotifications(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Open notifications",
            priority = 0,
        )

    fun createNewOpenQuickSettings(context: Context): OpenQuickSettings =
        OpenQuickSettings(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Open quick settings",
            priority = 0,
        )

    fun createNewScreenshot(context: Context): Screenshot =
        Screenshot(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Screenshot",
            roi = null,          // set in editor
            savePath = null,     // set in editor
            priority = 0,
        )

    fun createNewHideKeyboard(context: Context): HideKeyboard =
        HideKeyboard(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Hide keyboard",
            method = HideMethod.BACK_THEN_TAP_OUTSIDE,
            priority = 0,
        )

    fun createNewShowKeyboard(context: Context): ShowKeyboard =
        ShowKeyboard(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Show keyboard",
            positionType = defaultValues.clickPositionType(), // tap a field or a detected condition
            priority = 0,
        )

    fun createNewTypeText(context: Context): TypeText =
        TypeText(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Type",
            text = "", // editable in UI; non-null to pass isComplete()
            priority = 0,
        )

    fun createNewKeyEvent(context: Context): KeyEvent =
        KeyEvent(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = getEditedEventIdOrThrow(),
            name = "Key event",
            codes = emptyList(), // set in editor
            intervalMs = 50L,
            priority = 0,
        )

    private fun createNewLongPressFrom(from: LongPress, eventId: Identifier): LongPress {
        val mappedConditionId =
            if (from.positionType == PositionType.ON_DETECTED_CONDITION && from.onConditionId != null)
                eventCopyConditionIdMap[from.onConditionId]
            else null

        return from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
            onConditionId = mappedConditionId,
        )
    }

    private fun createNewScrollFrom(from: Scroll, eventId: Identifier): Scroll =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
        )

    private fun createNewScreenshotFrom(from: Screenshot, eventId: Identifier): Screenshot =
        from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
            // keep roi & savePath as-is so duplicates behave identically
        )

    private fun createNewShowKeyboardFrom(from: ShowKeyboard, eventId: Identifier): ShowKeyboard {
        val mappedConditionId =
            if (from.positionType == PositionType.ON_DETECTED_CONDITION && from.onConditionId != null)
                eventCopyConditionIdMap[from.onConditionId]
            else null

        return from.copy(
            id = actionsIdCreator.generateNewIdentifier(),
            eventId = eventId,
            name = "" + from.name,
            onConditionId = mappedConditionId,
        )
    }
    private fun isEventIdValidInEditedScenario(eventId: Identifier): Boolean =
        editor.getAllEditedEvents().find { eventId == it.id } != null

    private fun getEditedScenarioIdOrThrow(): Identifier =
        editor.editedScenario.value?.id
            ?: throw IllegalStateException("Can't create items without an edited scenario")

    private fun getEditedEventIdOrThrow(): Identifier =
        editor.currentEventEditor.value?.editedItem?.value?.id
            ?: throw IllegalStateException("Can't create items without an edited event")

    private fun getEditedActionIdOrThrow(): Identifier =
        editor.currentEventEditor.value?.actionsEditor?.editedItem?.value?.id
            ?: throw IllegalStateException("Can't create items without an edited action")

    private fun getEditedImageEventsCountOrThrow(): Int = editor.getEditedImageEventsCount()
}