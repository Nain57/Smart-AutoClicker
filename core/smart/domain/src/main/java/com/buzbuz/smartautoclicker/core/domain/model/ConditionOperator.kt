
package com.buzbuz.smartautoclicker.core.domain.model

import androidx.annotation.IntDef

/** Defines the operators to be applied between the conditions of an event. */
@IntDef(AND, OR)
@Retention(AnnotationRetention.SOURCE)
annotation class ConditionOperator
/** All conditions must be fulfilled. */
const val AND = 1
/** Only one of the conditions must be fulfilled. */
const val OR = 2
