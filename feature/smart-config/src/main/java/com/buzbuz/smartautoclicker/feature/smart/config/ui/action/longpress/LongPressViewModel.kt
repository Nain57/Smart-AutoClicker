package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.longpress

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

class LongPressViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {
    fun saveIfNeeded() { /* no-op for now */ }
    fun hasUnsavedModifications(): Boolean = false
}