/*
 * Copyright (C) 2022 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.base.dialog

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.domain.Repository

import kotlinx.coroutines.flow.MutableStateFlow

abstract class CopyViewModel<I>(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    protected val repository = Repository.getRepository(application)

    /** The currently searched action name. Null if no is. */
    protected val searchQuery = MutableStateFlow<String?>(null)
    /** The list of items from  the configured container. They are not all available yet in the database. */
    protected val itemsFromCurrentContainer = MutableStateFlow<List<I>?>(null)

    /**
     * Set the current container items.
     * @param items the items.
     */
    fun setItemsFromContainer(items: List<I>) {
        itemsFromCurrentContainer.value = items
    }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }
}