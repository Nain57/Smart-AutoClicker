
package com.buzbuz.smartautoclicker.feature.backup.domain

/** State of a backup import/export. */
sealed class Backup {

    /**
     * The backup is progressing.
     * @param progress the current scenario count, if available.
     * @param maxProgress the maximum progress value, if available.
     */
    data class Loading(val progress: Int? = null, val maxProgress: Int? = null) : Backup()

    /** The backup is verifying the imported data. */
    data object Verification : Backup()

    /**
     * The backup is completed.
     * @param successCount the number of scenario successfully imported/exported.
     * @param failureCount the number of failed import/export.
     * @param compatWarning true if the screen size of an imported scenario is different than this device screen size.
     */
    data class Completed(val successCount: Int, val failureCount: Int, val compatWarning: Boolean) : Backup()

    /** The backup has encountered an error and has stopped. */
    data object Error : Backup()
}