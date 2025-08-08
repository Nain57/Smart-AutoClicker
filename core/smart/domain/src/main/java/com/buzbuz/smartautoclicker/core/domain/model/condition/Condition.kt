
package com.buzbuz.smartautoclicker.core.domain.model.condition

import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable

sealed class Condition : Identifiable, Completable {

    abstract val eventId: Identifier
    abstract val name: String

    @CallSuper
    override fun isComplete(): Boolean =
        name.isNotEmpty()

    abstract fun hashCodeNoIds(): Int

}