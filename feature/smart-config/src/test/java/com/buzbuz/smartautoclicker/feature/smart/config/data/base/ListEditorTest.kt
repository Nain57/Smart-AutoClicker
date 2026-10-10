/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.data.base

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.interfaces.Prioritizable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ListEditorTest {

    private data class TestItem(
        override val id: Identifier,
        val name: String = "item",
        val complete: Boolean = true,
    ) : Identifiable, Completable {
        override fun isComplete(): Boolean = complete
    }

    private data class PrioritizedItem(
        override val id: Identifier,
        override var priority: Int,
    ) : Identifiable, Completable, Prioritizable {
        override fun isComplete(): Boolean = true
    }

    /** Editor whose items are only valid when a parent is set, to test the parent-aware validation hook. */
    private class ParentDependentListEditor(
        parentItem: MutableStateFlow<String?>,
    ) : ListEditor<TestItem, String>(parentItem = parentItem) {
        override fun itemCanBeSaved(item: TestItem?, parent: String?): Boolean =
            super.itemCanBeSaved(item, parent) && parent != null
    }

    private val parentFlow = MutableStateFlow<String?>(PARENT)
    private val listUpdates = mutableListOf<List<TestItem>>()

    private lateinit var editor: ListEditor<TestItem, String>

    @Before
    fun setUp() {
        editor = ListEditor(
            onListUpdated = { listUpdates.add(it) },
            canBeEmpty = false,
            parentItem = parentFlow,
        )
    }

    // --- List edition ---

    @Test
    fun `list state before edition started is not saveable`() = runTest {
        val state = editor.listState.first()

        assertNull(editor.editedList.value)
        assertNull(state.value)
        assertTrue(state.itemValidity.isEmpty())
        assertFalse(state.hasChanged)
        assertFalse(state.canBeSaved)
    }

    @Test
    fun `startEdition copies reference into edited list`() = runTest {
        val items = listOf(item(1), item(2))

        editor.startEdition(items)
        val state = editor.listState.first()

        assertEquals(items, editor.editedList.value)
        assertEquals(items, state.value)
        assertEquals(listOf(true, true), state.itemValidity)
        assertFalse(state.hasChanged)
        assertTrue(state.canBeSaved)
    }

    @Test
    fun `empty list is not saveable when canBeEmpty is false`() = runTest {
        editor.startEdition(emptyList())

        assertFalse(editor.listState.first().canBeSaved)
    }

    @Test
    fun `empty list is saveable when canBeEmpty is true`() = runTest {
        val emptyAllowedEditor = ListEditor<TestItem, String>(canBeEmpty = true, parentItem = parentFlow)

        emptyAllowedEditor.startEdition(emptyList())

        assertTrue(emptyAllowedEditor.listState.first().canBeSaved)
    }

    @Test
    fun `incomplete item makes list not saveable and is flagged in itemValidity`() = runTest {
        editor.startEdition(listOf(item(1), item(2, complete = false), item(3)))

        val state = editor.listState.first()

        assertEquals(listOf(true, false, true), state.itemValidity)
        assertFalse(state.canBeSaved)
    }

    @Test
    fun `list state hasChanged when edited list differs from reference`() = runTest {
        editor.startEdition(listOf(item(1), item(2)))

        editor.updateList(listOf(item(2), item(1)))

        assertTrue(editor.listState.first().hasChanged)
    }

    @Test
    fun `list state hasChanged is false when edited list is restored to reference`() = runTest {
        val items = listOf(item(1), item(2))
        editor.startEdition(items)
        editor.updateList(items.reversed())

        editor.updateList(items)

        assertFalse(editor.listState.first().hasChanged)
    }

    @Test
    fun `updateList with null does nothing`() {
        val items = listOf(item(1))
        editor.startEdition(items)

        editor.updateList(null)

        assertEquals(items, editor.editedList.value)
        assertTrue(listUpdates.isEmpty())
    }

    @Test
    fun `updateList notifies onListUpdated with new list`() {
        editor.startEdition(listOf(item(1)))
        val newItems = listOf(item(1), item(2))

        editor.updateList(newItems)

        assertEquals(newItems, editor.editedList.value)
        assertEquals(listOf(newItems), listUpdates)
    }

    @Test
    fun `updateList normalizes priorities of prioritizable items`() {
        val prioritizedEditor = ListEditor<PrioritizedItem, String>(parentItem = parentFlow)
        prioritizedEditor.startEdition(emptyList())

        prioritizedEditor.updateList(listOf(
            PrioritizedItem(Identifier(databaseId = 1), priority = 7),
            PrioritizedItem(Identifier(databaseId = 2), priority = 3),
            PrioritizedItem(Identifier(databaseId = 3), priority = 12),
        ))

        assertEquals(listOf(0, 1, 2), prioritizedEditor.editedList.value?.map { it.priority })
    }

    @Test
    fun `stopEdition clears list and item edition`() = runTest {
        editor.startEdition(listOf(item(1)))
        editor.startItemEdition(item(1))

        editor.stopEdition()

        assertNull(editor.editedList.value)
        assertNull(editor.editedItem.value)
        assertFalse(editor.isItemEditionStarted())
        assertFalse(editor.listState.first().canBeSaved)
    }

    // --- Item edition ---

    @Test
    fun `startItemEdition does nothing when list edition is not started`() {
        editor.startItemEdition(item(1))

        assertNull(editor.editedItem.value)
        assertFalse(editor.isItemEditionStarted())
    }

    @Test
    fun `startItemEdition exposes item with unchanged saveable state`() = runTest {
        val item = item(1)
        editor.startEdition(listOf(item))

        editor.startItemEdition(item)
        val state = editor.editedItemState.first()

        assertTrue(editor.isItemEditionStarted())
        assertEquals(item, editor.editedItem.value)
        assertEquals(item, state.value)
        assertFalse(state.hasChanged)
        assertTrue(state.canBeSaved)
    }

    @Test
    fun `edited item state before item edition is not saveable`() = runTest {
        editor.startEdition(listOf(item(1)))

        val state = editor.editedItemState.first()

        assertNull(state.value)
        assertFalse(state.hasChanged)
        assertFalse(state.canBeSaved)
    }

    @Test
    fun `updateEditedItem does nothing when item edition is not started`() {
        editor.startEdition(listOf(item(1)))

        editor.updateEditedItem(item(1, name = "renamed"))

        assertNull(editor.editedItem.value)
    }

    @Test
    fun `updateEditedItem marks item as changed`() = runTest {
        val item = item(1)
        editor.startEdition(listOf(item))
        editor.startItemEdition(item)
        val updated = item.copy(name = "renamed")

        editor.updateEditedItem(updated)
        val state = editor.editedItemState.first()

        assertEquals(updated, editor.editedItem.value)
        assertEquals(updated, state.value)
        assertTrue(state.hasChanged)
        assertTrue(state.canBeSaved)
    }

    @Test
    fun `updateEditedItem with incomplete item is not saveable`() = runTest {
        val item = item(1)
        editor.startEdition(listOf(item))
        editor.startItemEdition(item)

        editor.updateEditedItem(item.copy(complete = false))

        assertFalse(editor.editedItemState.first().canBeSaved)
    }

    @Test
    fun `updateEditedItem does not touch the list until upsert`() {
        val item = item(1)
        editor.startEdition(listOf(item))
        editor.startItemEdition(item)

        editor.updateEditedItem(item.copy(name = "renamed"))

        assertEquals(listOf(item), editor.editedList.value)
        assertTrue(listUpdates.isEmpty())
    }

    @Test
    fun `stopItemEdition clears edited item but keeps list`() {
        val items = listOf(item(1))
        editor.startEdition(items)
        editor.startItemEdition(item(1))

        editor.stopItemEdition()

        assertNull(editor.editedItem.value)
        assertFalse(editor.isItemEditionStarted())
        assertEquals(items, editor.editedList.value)
    }

    // --- Upsert ---

    @Test
    fun `upsertEditedItem does nothing when item edition is not started`() {
        val items = listOf(item(1))
        editor.startEdition(items)

        editor.upsertEditedItem()

        assertEquals(items, editor.editedList.value)
        assertTrue(listUpdates.isEmpty())
    }

    @Test
    fun `upsertEditedItem appends new item to list and stops item edition`() {
        editor.startEdition(listOf(item(1)))
        val newItem = item(2)
        editor.startItemEdition(newItem)

        editor.upsertEditedItem()

        assertEquals(listOf(item(1), newItem), editor.editedList.value)
        assertEquals(listOf(listOf(item(1), newItem)), listUpdates)
        assertFalse(editor.isItemEditionStarted())
    }

    @Test
    fun `upsertEditedItem replaces existing item with same id at same position`() {
        editor.startEdition(listOf(item(1), item(2), item(3)))
        editor.startItemEdition(item(2))
        val updated = item(2, name = "renamed")
        editor.updateEditedItem(updated)

        editor.upsertEditedItem()

        assertEquals(listOf(item(1), updated, item(3)), editor.editedList.value)
        assertFalse(editor.isItemEditionStarted())
    }

    @Test
    fun `upsertEditedItem treats database and temporary ids with same value as different items`() {
        val dbItem = TestItem(Identifier(databaseId = 5), name = "db")
        val tempItem = TestItem(Identifier(id = 5, asTemporary = true), name = "temp")
        editor.startEdition(listOf(dbItem))
        editor.startItemEdition(tempItem)

        editor.upsertEditedItem()

        assertEquals(listOf(dbItem, tempItem), editor.editedList.value)
    }

    // --- Delete ---

    @Test
    fun `deleteEditedItem does nothing when item edition is not started`() {
        val items = listOf(item(1))
        editor.startEdition(items)

        editor.deleteEditedItem()

        assertEquals(items, editor.editedList.value)
        assertTrue(listUpdates.isEmpty())
    }

    @Test
    fun `deleteEditedItem removes item from list and stops item edition`() {
        editor.startEdition(listOf(item(1), item(2), item(3)))
        editor.startItemEdition(item(2))

        editor.deleteEditedItem()

        assertEquals(listOf(item(1), item(3)), editor.editedList.value)
        assertEquals(listOf(listOf(item(1), item(3))), listUpdates)
        assertFalse(editor.isItemEditionStarted())
    }

    @Test
    fun `deleteEditedItem on item not in list only stops item edition`() {
        val items = listOf(item(1))
        editor.startEdition(items)
        editor.startItemEdition(item(2))

        editor.deleteEditedItem()

        assertEquals(items, editor.editedList.value)
        assertTrue(listUpdates.isEmpty())
        assertFalse(editor.isItemEditionStarted())
    }

    // --- All edited items ---

    @Test
    fun `allEditedItems is empty before edition`() = runTest {
        assertTrue(editor.allEditedItems.first().isEmpty())
        assertTrue(editor.getAllEditedItems().isEmpty())
    }

    @Test
    fun `allEditedItems contains list items only when no item is edited`() = runTest {
        val items = listOf(item(1), item(2))
        editor.startEdition(items)

        assertEquals(items, editor.allEditedItems.first())
        assertEquals(items, editor.getAllEditedItems())
    }

    @Test
    fun `allEditedItems appends new edited item not yet in list`() = runTest {
        editor.startEdition(listOf(item(1)))
        editor.startItemEdition(item(2))

        assertEquals(listOf(item(1), item(2)), editor.allEditedItems.first())
        assertEquals(listOf(item(1), item(2)), editor.getAllEditedItems())
    }

    @Test
    fun `allEditedItems does not duplicate edited item already in list`() = runTest {
        editor.startEdition(listOf(item(1), item(2)))
        editor.startItemEdition(item(2))
        editor.updateEditedItem(item(2, name = "renamed"))

        assertEquals(listOf(item(1), item(2)), editor.allEditedItems.first())
        assertEquals(listOf(item(1), item(2)), editor.getAllEditedItems())
    }

    // --- Parent-aware validation ---

    @Test
    fun `list and item validity follow parent through itemCanBeSaved override`() = runTest {
        val parentEditor = ParentDependentListEditor(parentFlow)
        val item = item(1)
        parentEditor.startEdition(listOf(item))
        parentEditor.startItemEdition(item)

        assertTrue(parentEditor.listState.first().canBeSaved)
        assertTrue(parentEditor.editedItemState.first().canBeSaved)

        parentFlow.value = null

        val listState = parentEditor.listState.first()
        assertFalse(listState.canBeSaved)
        assertEquals(listOf(false), listState.itemValidity)
        assertFalse(parentEditor.editedItemState.first().canBeSaved)
    }

    private fun item(id: Long, name: String = "item", complete: Boolean = true) =
        TestItem(Identifier(databaseId = id), name, complete)
}

private const val PARENT = "parent"
