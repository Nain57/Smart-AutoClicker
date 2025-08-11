
package com.buzbuz.smartautoclicker.feature.smart.debugging.domain

data class ConditionProcessingDebugInfo(
    val processingCount: Long = 0,
    val successCount: Long = 0,
    val totalProcessingTimeMs: Long = 0,
    val avgProcessingTimeMs: Long = 0,
    val minProcessingTimeMs: Long = 0,
    val maxProcessingTimeMs: Long = 0,
    val avgConfidenceRate: Double = 0.0,
    val minConfidenceRate: Double = 0.0,
    val maxConfidenceRate: Double = 0.0,
)