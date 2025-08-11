
package com.buzbuz.smartautoclicker.feature.smart.config.domain.model

data class EditedElementState<EditedType>(
    val value: EditedType?,
    val hasChanged: Boolean,
    val canBeSaved: Boolean,
)