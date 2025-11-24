package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.type

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

class TypeTextViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {
    fun saveIfNeeded() { /* no-op for now */ }
    fun hasUnsavedModifications(): Boolean = false
}