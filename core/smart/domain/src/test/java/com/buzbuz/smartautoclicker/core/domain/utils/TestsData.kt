
package com.buzbuz.smartautoclicker.core.domain.utils

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

internal fun Long.asIdentifier() = Identifier(
    databaseId = this,
    tempId = if (this == 0L) 1L else null,
)