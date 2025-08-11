
package com.buzbuz.smartautoclicker.feature.backup.data

import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario

/**
 * Notifies for a backup progress and state.
 *
 * @param onProgressChanged called for each scenario processed.
 * @param onCompleted called when the backup is completed.
 * @param onError called when the backup has encountered an error.
 * @param onVerification called when the backup is verifying the imported data.
 */
class BackupProgress(
    val onProgressChanged: suspend (current: Int?, max: Int?) -> Unit,
    val onCompleted: suspend (
        smartScenario: List<CompleteScenario>,
        failureCount: Int,
        compatWarning: Boolean,
    ) -> Unit,
    val onError: suspend () -> Unit,
    val onVerification: (suspend () -> Unit)? = null,
)