/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.externalaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.ExternalAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class ExternalActionEventConfigurationViewModel @Inject constructor(
    smartRepository: IRepository,
    private val codec: ExternalActionEventConfigurationCodec,
) : ViewModel() {

    val knownExternalActionNames: StateFlow<List<String>> =
        smartRepository.allActions
            .map { actions ->
                actions.asSequence()
                    .filterIsInstance<ExternalAction>()
                    .map { it.externalActionName.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sortedBy { it.lowercase() }
                    .toList()
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun decodeConfiguration(value: String?): ExternalActionEventConfiguration? = codec.decode(value)

    fun encodeConfiguration(externalActionName: String): String =
        codec.encode(ExternalActionEventConfiguration(externalActionName = externalActionName))
}
