
package com.buzbuz.smartautoclicker.feature.qstile.domain

internal data class QSTileDisplayInfo(
    val tileState: Int,
    val tileTitle: String,
    val tileSubTitle: String?,
    val scenarioId: Long? = null,
    val isSmart: Boolean? = null,
)
