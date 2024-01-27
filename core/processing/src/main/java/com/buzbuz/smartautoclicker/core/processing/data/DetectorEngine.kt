/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.media.projection.MediaProjectionManager
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.display.DisplayRecorder
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.detection.NativeDetector
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.processor.ProgressListener
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScenarioProcessor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Detects [Event] conditions on a display and execute its actions.
 *
 * In order to detect, you must start recording the screen to get images to detect on, this can be done by calling
 * [startScreenRecord]. Or, you can start the detection of a list of [Event] by using [startDetection].
 * The states of the recording and the detection are available in [state].
 * Once you no longer needs to capture or detect, call [stopDetection] or [stopScreenRecord] to release all processing resources.
 *
 * @param context the Android context.
 */
internal class DetectorEngine(context: Context) {

    /** Monitors the state of the screen. */
    private val displayMetrics = DisplayMetrics.getInstance(context)
    /** Listener upon orientation changes. */
    private val orientationListener = ::onOrientationChanged

    /** Record the screen and provide images via [DisplayRecorder.acquireLatestBitmap]. */
    private val displayRecorder = DisplayRecorder.getInstance()
    /** Process the events conditions to detect them on the screen. */
    private var scenarioProcessor: ScenarioProcessor? = null
    /** Detect the condition images on the screen image. */
    private var imageDetector: ImageDetector? = null
    /** The executor for the actions requiring an interaction with Android. */
    private var androidExecutor: AndroidExecutor? = null

    /** Coroutine scope for the image processing. */
    private var processingScope: CoroutineScope? = null
    /** Coroutine job for the image currently processed. */
    private var processingJob: Job? = null
    /** Coroutine job for the cleaning of the detection once stopped. */
    private var processingShutdownJob: Job? = null

    /** Backing property for [state].*/
    private val _state = MutableStateFlow(DetectorState.CREATED)
    /** Current state of the detector. */
    val state: StateFlow<DetectorState> = _state

    /**
     * Object to notify upon start/completion of detections steps.
     * Defined at detection start, reset to null at detection end.
     */
    private var detectionProgressListener: ProgressListener? = null

    /**
     * Start the screen detection.
     *
     * This requires the media projection permission code and its data intent, they both can be retrieved using the
     * results of the activity intent provided by [MediaProjectionManager.createScreenCaptureIntent] (this Intent shows
     * the dialog warning about screen recording privacy). Any attempt to call this method without the correct screen
     * capture intent result will leads to a crash.
     *
     * Once started, you can use [startDetection]. Once your are done, call [stopScreenRecord].
     *
     * @param context the Android context.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param androidExecutor the executor for the actions requiring an interaction with Android.
     */
    fun startScreenRecord(
        context: Context,
        resultCode: Int,
        data: Intent,
        androidExecutor: AndroidExecutor,
    ) {
        if (_state.value != DetectorState.CREATED) {
            Log.w(TAG, "startScreenRecord: Screen record is already started")
            return
        }
        _state.value = DetectorState.TRANSITIONING

        Log.i(TAG, "startScreenRecord")

        this.androidExecutor = androidExecutor
        processingScope = CoroutineScope(Dispatchers.IO)
        displayMetrics.addOrientationListener(orientationListener)

        processingScope?.launch {
            displayRecorder.apply {
                startProjection(context, resultCode, data) {
                    this@DetectorEngine.stopScreenRecord()
                }
                startScreenRecord(context, displayMetrics.screenSize)
            }

            _state.emit(DetectorState.RECORDING)
        }
    }

    /**
     * Start the screen detection.
     *
     * After calling this method, all [Image] displayed on the screen will be checked for the provided clicks conditions
     * fulfillment. For each image, the first event in the list that is detected will be notified through the provided
     * callback.
     * [state] should be RECORDING to capture. Detection can be stopped with [stopDetection] or [stopScreenRecord].
     *
     * @param bitmapSupplier provides the conditions bitmaps.
     * @param progressListener object to notify upon start/completion of detections steps.
     */
    fun startDetection(
        context: Context,
        scenario: Scenario,
        events: List<Event>,
        endConditions: List<EndCondition>,
        bitmapSupplier: suspend (String, Int, Int) -> Bitmap?,
        progressListener: ProgressListener? = null,
    ) {
        val executor = androidExecutor
        if (_state.value != DetectorState.RECORDING || executor == null) {
            Log.w(TAG, "startDetection: Screen record is not started.")
            return
        }

        val detector = NativeDetector.newInstance()
        if (detector == null) {
            Log.e(TAG, "startDetection: native library not found.")
            _state.value = DetectorState.ERROR_NATIVE_DETECTOR_LIB_NOT_FOUND
            return
        }

        _state.value = DetectorState.TRANSITIONING

        Log.i(TAG, "startDetection")

        processingScope?.launchProcessingJob {
            imageDetector = detector

            detectionProgressListener = progressListener
            progressListener?.onSessionStarted(context, scenario, events)

            scenarioProcessor = ScenarioProcessor(
                imageDetector = detector,
                detectionQuality = scenario.detectionQuality,
                randomize = scenario.randomize,
                events = events,
                bitmapSupplier = bitmapSupplier,
                androidExecutor = executor,
                endConditionOperator = scenario.endConditionOperator,
                endConditions =  endConditions,
                onStopRequested = { stopDetection() },
                progressListener  = progressListener,
            )

            processScreenImages()
        }
    }

    /**
     * Called when the orientation of the screen changes.
     * As we now have different screen metrics, we need to stop and start the virtual display with the correct one.
     *
     * @param context the Android context.
     */
    private fun onOrientationChanged(context: Context) {
        if (_state.value != DetectorState.DETECTING && _state.value != DetectorState.RECORDING) return

        Log.d(TAG, "onOrientationChanged")

        processingScope?.launch {
            if (_state.value == DetectorState.DETECTING) {
                processingJob?.cancelAndJoin()
            }

            displayRecorder.resizeDisplay(context, displayMetrics.screenSize)

            if (_state.value == DetectorState.DETECTING) {
                processingScope?.launchProcessingJob {
                    processScreenImages()
                }
            }
        }
    }

    /**
     * Stop the screen detection started with [startDetection].
     *
     * After a call to this method, the events provided in the start method will no longer be checked on the current
     * image. Note that this will not stop the screen recording, you should still call [stopScreenRecord] to completely
     * release the [DetectorEngine] resources.
     */
    fun stopDetection() {
        if (_state.value != DetectorState.DETECTING) {
            Log.w(TAG, "stopDetection: detection is not started.")
            return
        }
        _state.value = DetectorState.TRANSITIONING

        processingShutdownJob = processingScope?.launch {
            Log.i(TAG, "stopDetection")

            processingJob?.cancelAndJoin()
            processingJob = null
            imageDetector?.close()
            imageDetector = null
            scenarioProcessor = null
            detectionProgressListener?.onSessionEnded()
            detectionProgressListener = null

            _state.emit(DetectorState.RECORDING)
            processingShutdownJob = null
        }
    }

    /**
     * Stop the screen recording and the detection, if any.
     *
     * First, calls [stopDetection] if the detection was active. Then, stop the screen recording and release any related
     * resources.
     */
    fun stopScreenRecord() {
        if (_state.value == DetectorState.DETECTING) {
            stopDetection()
            stopRecording()
        } else if (_state.value == DetectorState.RECORDING) {
            stopRecording()
        }
    }

    private fun stopRecording() {
        Log.i(TAG, "stopScreenRecord")
        _state.value = DetectorState.TRANSITIONING

        displayMetrics.removeOrientationListener(orientationListener)
        processingScope?.launch {
            processingShutdownJob?.join()

            displayRecorder.stopProjection()
            androidExecutor = null
            _state.emit(DetectorState.CREATED)

            processingScope?.cancel()
            processingScope = null
        }
    }

    /** Process the latest images provided by the [DisplayRecorder]. */
    private suspend fun processScreenImages() {
        _state.emit(DetectorState.DETECTING)

        scenarioProcessor?.invalidateScreenMetrics()
        while (processingJob?.isActive == true) {
            displayRecorder.acquireLatestBitmap()?.let { screenFrame ->
                scenarioProcessor?.process(screenFrame)
            } ?: delay(NO_IMAGE_DELAY_MS)
        }
    }

    /** Clear this engine. It can't be used after this call. */
    fun clear() {
        if (_state.value != DetectorState.CREATED) {
            Log.w(TAG, "Clearing the detector but it was still started.")
            stopScreenRecord()
        }

        Log.i(TAG, "clear")


        _state.value != DetectorState.DESTROYED
    }

    /**
     * Creates a new job executing the provided job automatically once the job is effectively created.
     * This allows to check the job state correctly within the [block], even quickly after its start, as the [launch]
     * method with the [CoroutineStart.DEFAULT] starts the coroutine execution before returning the resulting [Job].
     *
     * The job will affected to the [processingJob] variable.
     *
     * @param block the coroutine code which will be invoked in the context of the provided scope.
     */
    private fun CoroutineScope.launchProcessingJob(block: suspend CoroutineScope.() -> Unit) {
        processingJob = launch(
            start = CoroutineStart.LAZY,
            block = block,
        )
        processingJob?.start()
    }
}

/** The different states of the [DetectorEngine]. */
internal enum class DetectorState {
    /** The engine is created and ready to be used. */
    CREATED,
    /**
     * The engine is transitioning between two states.
     * During this state, all call to the engine will be ignored.
     */
    TRANSITIONING,
    /** The screen is being recorded. */
    RECORDING,
    /** The screen is being recorded and the detection is running. */
    DETECTING,
    /** The engine is destroyed and can no longer be used. */
    DESTROYED,
    /** The native lib can't be loaded and the detection can't be used. */
    ERROR_NATIVE_DETECTOR_LIB_NOT_FOUND,
}

/**
 * Waiting delay after getting a null image.
 * This is to avoid spamming when there is no image.
 */
private const val NO_IMAGE_DELAY_MS = 20L

/** Tag for logs. */
private const val TAG = "DetectorEngine"