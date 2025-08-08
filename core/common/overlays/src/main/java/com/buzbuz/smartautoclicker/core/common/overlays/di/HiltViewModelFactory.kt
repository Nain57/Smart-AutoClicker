
package com.buzbuz.smartautoclicker.core.common.overlays.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.EntryPoints

inline fun <EP : Any> OverlayComponent.createHiltViewModelFactory(
    entryPoint: Class<EP>,
    crossinline creator: EP.() -> ViewModel,
) = object : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator(EntryPoints.get(this@createHiltViewModelFactory, entryPoint)) as T
    }
}