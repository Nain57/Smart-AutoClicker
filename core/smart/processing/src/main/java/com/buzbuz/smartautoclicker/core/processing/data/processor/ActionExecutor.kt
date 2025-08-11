
package com.buzbuz.smartautoclicker.core.processing.data.processor

import android.accessibilityservice.GestureDescription
import android.content.Intent as AndroidIntent
import android.graphics.Path
import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.extensions.buildSingleStroke
import com.buzbuz.smartautoclicker.core.base.extensions.nextIntInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.nextLongInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.safeLineTo
import com.buzbuz.smartautoclicker.core.base.extensions.safeMoveTo
import com.buzbuz.smartautoclicker.core.base.workarounds.UnblockGestureScheduler
import com.buzbuz.smartautoclicker.core.base.workarounds.buildUnblockGesture
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.putDomainExtra
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import com.buzbuz.smartautoclicker.core.domain.model.NotificationRequest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

import android.graphics.Rect
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

//import com.buzbuz.smartautoclicker.core.domain.model.action.LongPress
//import com.buzbuz.smartautoclicker.core.domain.model.action.Scroll
//import com.buzbuz.smartautoclicker.core.domain.model.action.Back
//import com.buzbuz.smartautoclicker.core.domain.model.action.Home
//import com.buzbuz.smartautoclicker.core.domain.model.action.Recents
//import com.buzbuz.smartautoclicker.core.domain.model.action.OpenNotifications
//import com.buzbuz.smartautoclicker.core.domain.model.action.OpenQuickSettings
//import com.buzbuz.smartautoclicker.core.domain.model.action.Screenshot
//import com.buzbuz.smartautoclicker.core.domain.model.action.HideKeyboard
//import com.buzbuz.smartautoclicker.core.domain.model.action.ShowKeyboard
//import com.buzbuz.smartautoclicker.core.domain.model.action.TypeText
//import com.buzbuz.smartautoclicker.core.domain.model.action.KeyEvent
//import com.buzbuz.smartautoclicker.core.domain.model.action.Axis
//import com.buzbuz.smartautoclicker.core.domain.model.action.HideMethod

/**
 * Execute the actions of an event.
 *
 * @param androidExecutor the executor for the actions requiring an interaction with Android.
 * @param processingState the state of the current processing (counters, enabled events...).
 * @param randomize true to randomize the actions values a bit (positions, timers...), false to be precise.
 */
internal class ActionExecutor(
    private val androidExecutor: SmartActionExecutor,
    private val processingState: ProcessingState,
    randomize: Boolean,
    unblockWorkaroundEnabled: Boolean = false,
) {

    init { androidExecutor.clearState() }

    private val random: Random? =
        if (randomize) Random(System.currentTimeMillis()) else null

    private val unblockGestureScheduler: UnblockGestureScheduler? =
        if (unblockWorkaroundEnabled) UnblockGestureScheduler()
        else null


    suspend fun onScenarioLoopFinished() {
        if (unblockGestureScheduler?.shouldTrigger() == true) {
            withContext(Dispatchers.Main) {
                Log.i(TAG, "Injecting unblock gesture")
                androidExecutor.executeGesture(
                    GestureDescription.Builder().buildUnblockGesture()
                )
            }
        }
    }

    suspend fun executeActions(event: Event, results: ConditionsResult? = null) {
        event.actions.forEach { action ->
            when (action) {
                is Click -> executeClick(event, action, results)
                is Swipe -> executeSwipe(action)
                is Pause -> executePause(action)
                is Intent -> executeIntent(action)
                is ToggleEvent -> executeToggleEvent(action)
                is ChangeCounter -> executeChangeCounter(action)
                is Notification -> executeNotification(event, action)

                is LongPress -> executeLongPress(event, action, results)
                is Scroll -> executeScroll(action)
                is Back -> executeBack()
                is Home -> executeHome()
                is Recents -> executeRecents()
                is OpenNotifications -> executeOpenNotifications()
                is OpenQuickSettings -> executeOpenQuickSettings()
                is Screenshot -> executeScreenshot(action)
                is HideKeyboard -> executeHideKeyboard(action)
                is ShowKeyboard -> executeShowKeyboard(event, action, results)
                is TypeText -> executeTypeText(action)
                is KeyEvent -> executeKeyEvent(action)
            }
        }
    }

    private suspend fun executeClick(event: Event, click: Click, results: ConditionsResult?) {
        val clickPath = when (click.positionType) {
            Click.PositionType.USER_SELECTED -> {
                click.position?.let { position ->
                    Path().apply { moveTo(position) }
                }
            }

            Click.PositionType.ON_DETECTED_CONDITION ->
                getOnConditionClickPath(event, click, results)
        } ?: return

        val clickGesture = GestureDescription.Builder().buildSingleStroke(
            clickPath, random.nextLongInOffsetIfNeeded(click.pressDuration!!, RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        )

        withContext(Dispatchers.Main) {
            androidExecutor.executeGesture(clickGesture)
        }
    }

    private fun getOnConditionClickPath(event: Event, click: Click, results: ConditionsResult?): Path? {
        if (event !is ImageEvent) return null

        val result = when {
            event.conditionOperator == OR -> results?.getFirstImageDetectedResult()
            click.clickOnConditionId != null -> results?.getImageConditionResult(click.clickOnConditionId!!.databaseId)
            else -> null
        }

        if (result == null) {
            Log.w(TAG, "Click is invalid, can't execute")
            return null
        }

        return Path().apply {
            moveTo(
                Point(
                    result.position.x + (click.clickOffset?.x ?: 0),
                    result.position.y + (click.clickOffset?.y ?: 0),
                )
            )
        }
    }

    /**
     * Execute the provided swipe.
     * @param swipe the swipe to be executed.
     */
    private suspend fun executeSwipe(swipe: Swipe) {
        val swipeGesture = GestureDescription.Builder().buildSingleStroke(
            path =
                if (swipe.from == null || swipe.to == null) return
                else Path().apply { line(swipe.from, swipe.to) },
            durationMs = random.nextLongInOffsetIfNeeded(swipe.swipeDuration!!, RANDOMIZATION_DURATION_MAX_OFFSET_MS),
        )

        withContext(Dispatchers.Main) {
            androidExecutor.executeGesture(swipeGesture)
        }
    }

    /**
     * Execute the provided pause.
     * @param pause the pause to be executed.
     */
    private suspend fun executePause(pause: Pause) {
        delay(random.nextLongInOffsetIfNeeded(pause.pauseDuration!!, RANDOMIZATION_DURATION_MAX_OFFSET_MS))
    }

    /**
     * Execute the provided intent.
     * @param intent the intent to be executed.
     */
    private suspend fun executeIntent(intent: Intent) {
        val androidIntent = AndroidIntent().apply {
            action = intent.intentAction!!
            flags = intent.flags!!

            intent.componentName?.let {
                component = intent.componentName
            }

            intent.extras?.forEach { putDomainExtra(it) }
        }

        if (intent.isBroadcast) {
            withContext(Dispatchers.Main) {
                androidExecutor.executeSendBroadcast(androidIntent)
            }
            delay(INTENT_BROADCAST_DELAY)
        } else {
            withContext(Dispatchers.Main) {
                androidExecutor.executeStartActivity(androidIntent)
            }
            delay(INTENT_START_ACTIVITY_DELAY)
        }
    }

    /**
     * Execute the provided toggle event.
     * @param toggleEvent the toggleEvent to be executed.
     */
    private fun executeToggleEvent(toggleEvent: ToggleEvent) {
        if (toggleEvent.toggleAll) {
            when (toggleEvent.toggleAllType) {
                ToggleEvent.ToggleType.ENABLE -> processingState.enableAll()
                ToggleEvent.ToggleType.DISABLE -> processingState.disableAll()
                ToggleEvent.ToggleType.TOGGLE -> processingState.toggleAll()
                null -> Unit
            }

            return
        }

        toggleEvent.eventToggles.forEach { eventToggle ->
            when (eventToggle.toggleType) {
                ToggleEvent.ToggleType.ENABLE -> processingState.enableEvent(eventToggle.targetEventId!!.databaseId)
                ToggleEvent.ToggleType.DISABLE -> processingState.disableEvent(eventToggle.targetEventId!!.databaseId)
                ToggleEvent.ToggleType.TOGGLE -> processingState.toggleEvent(eventToggle.targetEventId!!.databaseId)
            }
        }
    }

    /**
     * Execute the provided change counter.
     * @param changeCounter the changeCounter action to be executed.
     */
    private fun executeChangeCounter(changeCounter: ChangeCounter) {
        val oldValue = processingState.getCounterValue(changeCounter.counterName) ?: return

        val operandValue = when (val operationValue = changeCounter.operationValue) {
            is CounterOperationValue.Counter -> processingState.getCounterValue(operationValue.value) ?: 0
            is CounterOperationValue.Number -> operationValue.value
        }

        processingState.setCounterValue(
            counterName = changeCounter.counterName,
            value = when (changeCounter.operation) {
                ChangeCounter.OperationType.ADD -> oldValue + operandValue
                ChangeCounter.OperationType.MINUS -> oldValue - operandValue
                ChangeCounter.OperationType.SET -> operandValue
            }
        )
    }

    private fun executeNotification(event: Event, notification: Notification) {
        val message = when (notification.messageType) {
            Notification.MessageType.TEXT -> notification.messageText
            Notification.MessageType.COUNTER_VALUE -> {
                val counterValue = processingState.getCounterValue(notification.messageCounterName) ?: return
                notification.messageCounterName + " = " + counterValue
            }
        }

        androidExecutor.executeNotification(
            NotificationRequest(
                actionId = notification.id.databaseId,
                title = notification.name ?: "Klick'r",
                message = message,
                eventId = event.id.databaseId,
                groupName = event.name,
                importance = notification.channelImportance,
            )
        )
    }


    private fun Path.moveTo(position: Point) {
        if (random == null) safeMoveTo(position.x, position.y)
        else safeMoveTo(
            random.nextIntInOffset(position.x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
            random.nextIntInOffset(position.y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        )
    }

    private fun Path.line(from: Point?, to: Point?) {
        if (from == null || to == null) return

        moveTo(from)
        lineTo(to)
    }

    private fun Path.lineTo(position: Point) {
        if (random == null) safeLineTo(position.x, position.y)
        else safeLineTo(
            random.nextIntInOffset(position.x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
            random.nextIntInOffset(position.y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        )
    }

    private suspend fun executeLongPress(event: Event, lp: LongPress, results: ConditionsResult?) {
        val path = when (lp.positionType) {
            Click.PositionType.USER_SELECTED -> lp.position?.let { Path().apply { moveTo(it) } }
            Click.PositionType.ON_DETECTED_CONDITION -> getOnConditionClickPathForLongPress(event, lp, results)
        } ?: return

        val press = random.nextLongInOffsetIfNeeded(lp.holdDuration!!, RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        val gesture = GestureDescription.Builder().buildSingleStroke(path, press)

        withContext(Dispatchers.Main) { androidExecutor.executeGesture(gesture) }
    }

    private fun getOnConditionClickPathForLongPress(event: Event, lp: LongPress, results: ConditionsResult?): Path? {
        if (event !is ImageEvent) return null
        val result = when {
            event.conditionOperator == OR -> results?.getFirstImageDetectedResult()
            lp.onConditionId != null -> results?.getImageConditionResult(lp.onConditionId!!.databaseId)
            else -> null
        } ?: return null

        return Path().apply {
            moveTo(Point(
                result.position.x + (lp.offset?.x ?: 0),
                result.position.y + (lp.offset?.y ?: 0),
            ))
        }
    }

    private suspend fun executeScroll(scroll: Scroll) {
        val bounds: Rect = androidExecutor.getScreenBounds() ?: Rect(0, 0, 1080, 1920) // << add in SmartActionExecutor
        val (from, to) = computeScrollPoints(bounds, scroll.axis!!, scroll.distancePercent!!)
        val path = Path().apply { line(from, to) }
        val duration = random.nextLongInOffsetIfNeeded(scroll.duration!!, RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        val gesture = GestureDescription.Builder().buildSingleStroke(path, duration)

        withContext(Dispatchers.Main) { androidExecutor.executeGesture(gesture) }
        if (scroll.stutter) delay((120..220).random().toLong())
    }

    private fun computeScrollPoints(screen: Rect, axis: Axis, distancePercent: Float): Pair<Point, Point> {
        val w = screen.width().toFloat()
        val h = screen.height().toFloat()
        val cx = screen.left + (w * 0.5f)
        val cy = screen.top + (h * 0.6f) // below center reads more human

        val dist = (when (axis) {
            Axis.UP, Axis.DOWN -> h
            Axis.LEFT, Axis.RIGHT -> w
        } * distancePercent.coerceIn(0.1f, 0.9f)).toInt()

        return when (axis) {
            Axis.UP    -> Point(cx.toInt(), cy.toInt()) to Point(cx.toInt(), (cy - dist).toInt())
            Axis.DOWN  -> Point(cx.toInt(), cy.toInt()) to Point(cx.toInt(), (cy + dist).toInt())
            Axis.LEFT  -> Point(cx.toInt(), cy.toInt()) to Point((cx - dist).toInt(), cy.toInt())
            Axis.RIGHT -> Point(cx.toInt(), cy.toInt()) to Point((cx + dist).toInt(), cy.toInt())
        }
    }

    private suspend fun executeBack()            { withContext(Dispatchers.Main) { androidExecutor.executeGlobalBack() } }
    private suspend fun executeHome()            { withContext(Dispatchers.Main) { androidExecutor.executeGlobalHome() } }
    private suspend fun executeRecents()         { withContext(Dispatchers.Main) { androidExecutor.executeGlobalRecents() } }
    private suspend fun executeOpenNotifications(){ withContext(Dispatchers.Main) { androidExecutor.executeGlobalNotifications() } }
    private suspend fun executeOpenQuickSettings(){ withContext(Dispatchers.Main) { androidExecutor.executeGlobalQuickSettings() } }

    private suspend fun executeScreenshot(shot: Screenshot) {
        if (Build.VERSION.SDK_INT < 30) return
        val roi = shot.roi?.let { Rect(it.left, it.top, it.left + it.width, it.top + it.height) }
        withContext(Dispatchers.Main) {
            androidExecutor.executeScreenshot(roi, shot.savePath) // add in SmartActionExecutor
        }
    }

    private suspend fun executeHideKeyboard(hk: HideKeyboard) {
        withContext(Dispatchers.Main) {
            when (hk.method) {
                HideMethod.BACK -> androidExecutor.executeGlobalBack()
                HideMethod.TAP_OUTSIDE -> androidExecutor.tapSafeArea() // add small helper: tap near (40,80)
                HideMethod.BACK_THEN_TAP_OUTSIDE -> {
                    androidExecutor.executeGlobalBack()
                    delay(100)
                    androidExecutor.tapSafeArea()
                }
            }
        }
    }

    private suspend fun executeShowKeyboard(event: Event, sk: ShowKeyboard, results: ConditionsResult?) {
        // It’s just a tap on a focusable field; reuse click targeting and a short press.
        val path = when (sk.positionType) {
            Click.PositionType.USER_SELECTED -> sk.position?.let { Path().apply { moveTo(it) } }
            Click.PositionType.ON_DETECTED_CONDITION -> {
                if (event !is ImageEvent) null else {
                    val result = when {
                        event.conditionOperator == OR -> results?.getFirstImageDetectedResult()
                        sk.onConditionId != null -> results?.getImageConditionResult(sk.onConditionId!!.databaseId)
                        else -> null
                    } ?: return
                    Path().apply { moveTo(Point(
                        result.position.x + (sk.offset?.x ?: 0),
                        result.position.y + (sk.offset?.y ?: 0),
                    )) }
                }
            }
        } ?: return

        val gesture = GestureDescription.Builder().buildSingleStroke(path, 60L)
        withContext(Dispatchers.Main) { androidExecutor.executeGesture(gesture) }
    }

    private suspend fun executeTypeText(tt: TypeText) {
        withContext(Dispatchers.Main) {
            androidExecutor.executeSetText(tt.text!!) // add in SmartActionExecutor; uses ACTION_SET_TEXT on focused node
        }
    }

    private suspend fun executeKeyEvent(ke: KeyEvent) {
        withContext(Dispatchers.Main) {
            androidExecutor.executeImeKeySequence(ke.codes!!, ke.intervalMs ?: 50L) // add in SmartActionExecutor
        }
    }



    private fun Random?.nextLongInOffsetIfNeeded(value: Long, offset: Long): Long =
        this?.nextLongInOffset(value, offset) ?: value
}

/** Tag for logs. */
private const val TAG = "ActionExecutor"
/** Waiting delay after a start activity to avoid overflowing the system. */
private const val INTENT_START_ACTIVITY_DELAY = 1000L
/** Waiting delay after a broadcast to avoid overflowing the system. */
private const val INTENT_BROADCAST_DELAY = 100L

private const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
private const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5L

private const val DEFAULT_LONG_PRESS_MS = 600L
private const val DEFAULT_SCROLL_DURATION_MS = 350L
