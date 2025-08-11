
package com.buzbuz.smartautoclicker.feature.smart.debugging.domain

data class ProcessingDebugInfo(
    val processingCount: Long = 0,
    val successCount: Long = 0,
    val totalProcessingTimeMs: Long = 0,
    val avgProcessingTimeMs: Long = 0,
    val minProcessingTimeMs: Long = 0,
    val maxProcessingTimeMs: Long = 0,
)
