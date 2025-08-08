
package com.buzbuz.smartautoclicker.core.processing.data

import android.content.Context
import android.content.Intent
import android.media.Image
import android.media.projection.MediaProjectionManager
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.display.recorder.DisplayRecorder
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.detection.NativeDetector
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScenarioProcessor
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects [ImageEvent] conditions on a display and execute its actions.
 *
 * In order to detect, you must start recording the screen to get images to detect on, this can be done by calling
 * [startScreenRecord]. Or, you can start the detection of a list of [ImageEvent] by using [startDetection].
 * The states of the recording and the detection are available in [state].
 * Once you no longer needs to capture or detect, call [stopDetection] or [stopScreenRecord] to release all processing resources.
 */
@Singleton
class DetectorEngine @Inject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val displayConfigManager: DisplayConfigManager,
    private val bitmapRepository: BitmapRepository,
    private val scalingManager: ScalingManager,
    private val displayRecorder: DisplayRecorder,
    private val settingsRepository: SettingsRepository,
    private val appComponentsProvider: AppComponentsProvider,
) {

    /** Process the events conditions to detect them on the screen. */
    private var scenarioProcessor: ScenarioProcessor? = null
    /** Detect the condition images on the screen image. */
    private var imageDetector: ImageDetector? = null
    /** The executor for the actions requiring an interaction with Android. */
    private var androidExecutor: SmartActionExecutor? = null

    /** Coroutine scope for the image processing. */
    private var processingScope: CoroutineScope? = null
    /** Coroutine job for the image currently processed. */
    private var processingJob: Job? = null
    /** Coroutine job for the cleaning of the detection once stopped. */
    private var processingShutdownJob: Job? = null

    private val screenOrientationListener: (Context) -> Unit = { onScreenOrientationChanged() }

    /** Backing property for [state].*/
    private val _state = MutableStateFlow(DetectorState.CREATED)
    /** Current state of the detector. */
    internal val state: StateFlow<DetectorState> = _state

    /**
     * Object to notify upon start/completion of detections steps.
     * Defined at detection start, reset to null at detection end.
     */
    private var detectionProgressListener: ScenarioProcessingListener? = null

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
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param androidExecutor the executor for the actions requiring an interaction with Android.
     * @param onRecordingStopped called when the screen recording is no longer running and a new request for media
     * projection should be done.
     */
    internal fun startScreenRecord(
        resultCode: Int,
        data: Intent,
        androidExecutor: SmartActionExecutor,
        onRecordingStopped: (() -> Unit)?,
    ) {
        if (_state.value != DetectorState.CREATED) {
            Log.w(TAG, "startScreenRecord: Screen record is already started")
            return
        }

        val displaySize = displayConfigManager.displayConfig.sizePx
        if (displaySize.x <= 0 || displaySize.y <= 0) {
            Log.w(TAG, "startScreenRecord: Invalid display size $displaySize")
            return
        }

        _state.value = DetectorState.TRANSITIONING

        Log.i(TAG, "startScreenRecord")

        this.androidExecutor = androidExecutor
        processingScope = CoroutineScope(ioDispatcher)

        displayConfigManager.addOrientationListener(screenOrientationListener)

        processingScope?.launch {
            displayRecorder.apply {
                startProjection(resultCode, data) {
                    Log.w(TAG, "projection lost")
                    this@DetectorEngine.stopScreenRecord()
                    onRecordingStopped?.invoke()
                }
                startScreenRecord(displaySize)
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
     * @param progressListener object to notify upon start/completion of detections steps.
     */
    internal fun startDetection(
        context: Context,
        scenario: Scenario,
        imageEvents: List<ImageEvent>,
        triggerEvents: List<TriggerEvent>,
        progressListener: ScenarioProcessingListener? = null,
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
            // Clear image cache and compute scaling info for detection
            bitmapRepository.clearCache()


            // Set the display projection to the scaled size
            displayRecorder.resizeDisplay(
                displaySize = scalingManager.startScaling(
                    quality = scenario.detectionQuality.toDouble(),
                    screenEvents = imageEvents,
                )
            )

            // Setup native detector
            imageDetector = detector
            detector.init()

            // Setup listeners
            detectionProgressListener = progressListener
            progressListener?.onSessionStarted(context, scenario, imageEvents, triggerEvents)

            // Instantiate the processor and initialize its detection state.
            scenarioProcessor = ScenarioProcessor(
                processingTag = appComponentsProvider.originalAppId,
                imageDetector = detector,
                scalingManager = scalingManager,
                randomize = scenario.randomize,
                imageEvents = imageEvents,
                triggerEvents = triggerEvents,
                bitmapSupplier = bitmapRepository::getImageConditionBitmap,
                androidExecutor = executor,
                unblockWorkaroundEnabled = settingsRepository.isInputBlockWorkaroundEnabled(),
                onStopRequested = { stopDetection() },
                progressListener  = progressListener,
            )
            scenarioProcessor?.onScenarioStart(context)

            processScreenImages()
        }
    }

    /**
     * Called when the orientation of the screen changes.
     * As we now have different screen metrics, we need to stop and start the virtual display with the correct one.
     */
    private fun onScreenOrientationChanged() {
        if (_state.value != DetectorState.DETECTING && _state.value != DetectorState.RECORDING) return

        Log.d(TAG, "onOrientationChanged")

        processingScope?.launch {
            if (_state.value == DetectorState.DETECTING) {
                processingJob?.cancelAndJoin()
                detectionProgressListener?.onImageEventProcessingCancelled()
            }


            displayRecorder.resizeDisplay(
                displaySize = scalingManager.refreshScaling(),
            )

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
    internal fun stopDetection() {
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
            scenarioProcessor?.onScenarioEnd()
            scenarioProcessor = null
            detectionProgressListener?.onSessionEnded()
            detectionProgressListener = null

            scalingManager.stopScaling()
            displayRecorder.resizeDisplay(displayConfigManager.displayConfig.sizePx)

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
    internal fun stopScreenRecord() {
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

        processingScope?.launch {
            processingShutdownJob?.join()

            displayConfigManager.removeOrientationListener(screenOrientationListener)
            displayRecorder.stopProjection()
            androidExecutor = null
            _state.emit(DetectorState.CREATED)

            processingScope?.cancel()
            processingScope = null
        }
    }

    /** Clear this engine. It can't be used after this call. */
    internal fun clear() {
        if (_state.value != DetectorState.CREATED) {
            Log.w(TAG, "Clearing the detector but it was still started.")
            stopScreenRecord()
        }

        Log.i(TAG, "clear")

        _state.value != DetectorState.DESTROYED
    }

    /** Process the latest images provided by the [DisplayRecorder]. */
    private suspend fun processScreenImages() {
        _state.emit(DetectorState.DETECTING)

        while (processingJob?.isActive == true) {
            displayRecorder.acquireLatestBitmap()?.let { screenFrame ->
                scenarioProcessor?.process(screenFrame)
            } ?: delay(NO_IMAGE_DELAY_MS)
        }
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