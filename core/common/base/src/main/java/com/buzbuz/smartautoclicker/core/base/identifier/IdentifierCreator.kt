
package com.buzbuz.smartautoclicker.core.base.identifier

class IdentifierCreator {

    /** The last generated domain id for an item. */
    private var lastGeneratedDomainId: Long = 0

    fun generateNewIdentifier(): Identifier =
        Identifier(databaseId = DATABASE_ID_INSERTION, tempId = ++lastGeneratedDomainId)

    fun resetIdCount() {
        lastGeneratedDomainId = 0
    }
}