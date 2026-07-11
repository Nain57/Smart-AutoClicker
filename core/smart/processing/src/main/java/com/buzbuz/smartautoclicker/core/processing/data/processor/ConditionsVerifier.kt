/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.data.processor

import android.graphics.Bitmap

import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.detection.NumberFormatType as DetectionNumberFormatType
import com.buzbuz.smartautoclicker.core.domain.model.condition.NumberFormatType as DomainNumberFormatType
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScreenConditionScalingInfo
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult

import kotlinx.coroutines.yield

import kotlin.math.abs

private const val DOUBLE_EQUALS_EPSILON = 1e-9

internal class ConditionsVerifier(
    private val state: ProcessingState,
    private val imageDetector: ImageDetector,
    private val scalingManager: ScalingManager,
    private val bitmapSupplier: suspend (String, Int, Int) -> Bitmap?,
    private val progressListener: SmartProcessingListener? = null,
) {

    /** List of results for the last call to verifyConditions. */
    private val verificationResults: ConditionsResults = ConditionsResults()

    /**
     * Set only during a [verifyConditions], it contains the system time at verification start.
     * This allows to use the same reference time for all conditions during the same verification loop.
     */
    private var currentVerificationTsMs: Long? = null

    suspend fun verifyConditions(@ConditionOperator operator: Int, conditions: List<Condition>): ConditionsResults {
        verificationResults.reset()
        currentVerificationTsMs = System.currentTimeMillis()

        var verificationResult: ProcessedConditionResult
        for (condition in conditions) {
            verificationResult = verifyCondition(condition)
            verificationResults.addResult(condition.getValidId(), verificationResult)

            if (operator == OR && verificationResult.isFulfilled) {
                verificationResults.setFulfilledState(true)
                return verificationResults
            }
            if (operator == AND && !verificationResult.isFulfilled) {
                verificationResults.setFulfilledState(false)
                return verificationResults
            }

            yield()
        }

        verificationResults.setFulfilledState(operator == AND)
        return verificationResults
    }

    private suspend fun verifyCondition(condition: Condition): ProcessedConditionResult =
        when (condition) {
            is ScreenCondition.Color -> verifyColorCondition(condition)
            is ScreenCondition.Image -> verifyImageCondition(condition)
            is ScreenCondition.Text -> verifyTextCondition(condition)
            is ScreenCondition.Number -> verifyNumberCondition(condition)
            is TriggerCondition -> condition.toConditionResult(verifyTriggerCondition(condition))
        }

    private fun verifyTriggerCondition(condition: TriggerCondition): Boolean =
        when (condition) {
            is TriggerCondition.OnBroadcastReceived -> verifyOnBroadcastReceived(condition)
            is TriggerCondition.OnCounterCountReached -> verifyOnCounterReached(condition)
            is TriggerCondition.OnTimerReached -> verifyOnTimerReached(condition)
        }

    private fun verifyOnBroadcastReceived(condition: TriggerCondition.OnBroadcastReceived): Boolean =
        state.isBroadcastReceived(condition)

    private fun verifyOnCounterReached(condition: TriggerCondition.OnCounterCountReached): Boolean =
        state.getCounterValue(condition.counterName)?.let { counterValue ->

            val operandValue = when (val operationValue = condition.counterValue) {
                is CounterOperationValue.Counter -> state.getCounterValue(operationValue.value) ?: 0.0
                is CounterOperationValue.Number -> operationValue.value
            }

            when (condition.comparisonOperation) {
                ComparisonOperation.GREATER -> counterValue > operandValue
                ComparisonOperation.GREATER_OR_EQUALS -> counterValue >= operandValue
                ComparisonOperation.EQUALS -> abs(counterValue - operandValue) < DOUBLE_EQUALS_EPSILON
                ComparisonOperation.LOWER_OR_EQUALS -> counterValue <= operandValue
                ComparisonOperation.LOWER -> counterValue < operandValue
            }
        } ?: false

    private fun verifyOnTimerReached(condition: TriggerCondition.OnTimerReached): Boolean {
        val currentTsMs = currentVerificationTsMs ?: return false
        val timerEndMs = state.getTimerEndMs(condition.getValidId()) ?: return false

        return if (currentTsMs > timerEndMs) {
            if (condition.restartWhenReached) state.setTimerStartToNow(condition)
            else state.setTimerToDisabled(condition.getValidId())
            true
        } else false
    }

    private fun verifyColorCondition(condition: ScreenCondition.Color): ProcessedConditionResult.Screen {
        progressListener?.onScreenConditionProcessingStarted()

        val conditionScalingInfo = scalingManager
            .getScreenConditionScalingInfo(condition) as? ScreenConditionScalingInfo.Color
            ?: return condition.toInvalidConditionResult()

        val detectionResult = imageDetector.detectColor(
            conditionColor = condition.color,
            detectionArea = conditionScalingInfo.detectionArea,
            threshold = condition.threshold,
        )

        val result = ProcessedConditionResult.Screen(
            isFulfilled = detectionResult.isDetected == condition.shouldBeDetected,
            haveBeenDetected = detectionResult.isDetected,
            condition = condition,
            confidenceRate = detectionResult.confidenceRate,
            position = scalingManager.scaleUpDetectionResult(detectionResult.position),
            size = scalingManager.scaleUpDetectionResult(detectionResult.size),
        )

        progressListener?.onScreenConditionProcessingCompleted(result)
        return result
    }

    private suspend fun verifyImageCondition(condition: ScreenCondition.Image): ProcessedConditionResult.Screen {
        progressListener?.onScreenConditionProcessingStarted()

        val conditionScalingInfo = scalingManager
            .getScreenConditionScalingInfo(condition) as? ScreenConditionScalingInfo.Image
            ?: return condition.toInvalidConditionResult()

        val bitmap = bitmapSupplier(
            condition.path,
            conditionScalingInfo.imageArea.width(),
            conditionScalingInfo.imageArea.height(),
        )

        val result = bitmap?.let { conditionBitmap ->
            val detectionResult = imageDetector.detectImage(
                conditionBitmap = conditionBitmap,
                conditionWidth = conditionScalingInfo.imageArea.width(),
                conditionHeight = conditionScalingInfo.imageArea.height(),
                detectionArea = conditionScalingInfo.detectionArea,
                threshold = condition.threshold,
            )

            ProcessedConditionResult.Screen(
                isFulfilled = detectionResult.isDetected == condition.shouldBeDetected,
                haveBeenDetected = detectionResult.isDetected,
                condition = condition,
                position = scalingManager.scaleUpDetectionResult(detectionResult.position),
                confidenceRate = detectionResult.confidenceRate,
                size = scalingManager.scaleUpDetectionResult(detectionResult.size),
            )
        } ?: condition.toInvalidConditionResult()

        progressListener?.onScreenConditionProcessingCompleted(result)
        return result
    }

    private fun verifyNumberCondition(condition: ScreenCondition.Number): ProcessedConditionResult.Screen {
        progressListener?.onScreenConditionProcessingStarted()

        val conditionScalingInfo = scalingManager
            .getScreenConditionScalingInfo(condition) as? ScreenConditionScalingInfo.Number
            ?: return condition.toInvalidConditionResult()

        val detectionResult = imageDetector.detectNumber(
            detectionArea = conditionScalingInfo.detectionArea,
            threshold = condition.threshold,
            numberFormatType = condition.numberFormatType.toDetectionNumberFormatType(),
        )

        val numberDetected: Double? = detectionResult.numberDetected
        val result =
            if (!detectionResult.isDetected || numberDetected == null) condition.toInvalidConditionResult()
            else {
                val operandValue = when (val operationValue = condition.counterValue) {
                    is CounterOperationValue.Counter -> state.getCounterValue(operationValue.value) ?: 0.0
                    is CounterOperationValue.Number -> operationValue.value
                }

                val comparisonResult = when (condition.comparisonOperation) {
                    ComparisonOperation.GREATER -> numberDetected > operandValue
                    ComparisonOperation.GREATER_OR_EQUALS -> numberDetected >= operandValue
                    ComparisonOperation.EQUALS -> abs(numberDetected - operandValue) < DOUBLE_EQUALS_EPSILON
                    ComparisonOperation.LOWER_OR_EQUALS -> numberDetected <= operandValue
                    ComparisonOperation.LOWER -> numberDetected < operandValue
                }

                ProcessedConditionResult.Screen(
                    isFulfilled = comparisonResult,
                    haveBeenDetected = true,
                    condition = condition,
                    position = scalingManager.scaleUpDetectionResult(detectionResult.position),
                    confidenceRate = detectionResult.confidenceRate,
                    size = scalingManager.scaleUpDetectionResult(detectionResult.size),
                )
            }

        progressListener?.onScreenConditionProcessingCompleted(result)
        return result
    }

    private fun verifyTextCondition(condition: ScreenCondition.Text): ProcessedConditionResult.Screen {
        progressListener?.onScreenConditionProcessingStarted()

        val conditionScalingInfo = scalingManager
            .getScreenConditionScalingInfo(condition) as? ScreenConditionScalingInfo.Text
            ?: return condition.toInvalidConditionResult()

        val detectionResult = imageDetector.detectText(
            conditionText = condition.text,
            recognitionModelId = condition.alphabet.name,
            detectionArea = conditionScalingInfo.detectionArea,
            threshold = condition.threshold,
        )

        val result = ProcessedConditionResult.Screen(
            isFulfilled = detectionResult.isDetected == condition.shouldBeDetected,
            haveBeenDetected = detectionResult.isDetected,
            condition = condition,
            position = scalingManager.scaleUpDetectionResult(detectionResult.position),
            confidenceRate = detectionResult.confidenceRate,
            size = scalingManager.scaleUpDetectionResult(detectionResult.size),
        )

        progressListener?.onScreenConditionProcessingCompleted(result)
        return result
    }

    private fun ScreenCondition.toInvalidConditionResult(): ProcessedConditionResult.Screen =
        ProcessedConditionResult.Screen(
            isFulfilled = false,
            haveBeenDetected = false,
            condition = this,
            confidenceRate = 0.0,
            position = null,
            size = null,
        )

    private fun TriggerCondition.toConditionResult(positive: Boolean): ProcessedConditionResult.Trigger =
        ProcessedConditionResult.Trigger(
            isFulfilled = positive,
            condition = this,
        )
}

private fun DomainNumberFormatType.toDetectionNumberFormatType(): DetectionNumberFormatType =
    when (this) {
        DomainNumberFormatType.AUTO -> DetectionNumberFormatType.AUTO
        DomainNumberFormatType.DOT_DECIMAL -> DetectionNumberFormatType.DOT_DECIMAL
        DomainNumberFormatType.COMMA_DECIMAL -> DetectionNumberFormatType.COMMA_DECIMAL
    }
