
package com.buzbuz.smartautoclicker.core.base.migrations

sealed class SQLiteColumn<T : Any> {

    abstract val name: String
    abstract val isNotNull: kotlin.Boolean
    internal abstract val typeSQLLite: String
    sealed class Default<T : Any> : SQLiteColumn<T>() {
        abstract val defaultValue: String?
    }


    data class Int(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<kotlin.Int>() {
        override val typeSQLLite: String = "INTEGER"
    }

    data class Long(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<kotlin.Long>() {
        override val typeSQLLite: String = "INTEGER"
    }

    data class Boolean(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<kotlin.Boolean>() {
        override val typeSQLLite: String = "INTEGER"
    }

    data class Text(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<String>() {
        override val typeSQLLite: String = "TEXT"
    }

    data class PrimaryKey(
        override val name: String = "id",
    ) : SQLiteColumn<kotlin.Long>() {
        override val typeSQLLite: String = "INTEGER"
        override val isNotNull: kotlin.Boolean = true
    }

    data class ForeignKey(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        val referencedTable: String,
        val referencedColumn: String,
        @androidx.room.ForeignKey.Action val updateAction: kotlin.Int = androidx.room.ForeignKey.NO_ACTION,
        @androidx.room.ForeignKey.Action val deleteAction: kotlin.Int = androidx.room.ForeignKey.NO_ACTION,
    ) : SQLiteColumn<kotlin.Long>() {

        override val typeSQLLite: String = "INTEGER"

        internal val updateActionSQLite: String
            get() = updateAction.toSQLiteForeignKeyAction()

        internal val deleteActionSQLite: String
            get() = deleteAction.toSQLiteForeignKeyAction()

        private fun kotlin.Int.toSQLiteForeignKeyAction(): String =
            when (this) {
                androidx.room.ForeignKey.NO_ACTION -> "NO ACTION"
                androidx.room.ForeignKey.RESTRICT -> "RESTRICT"
                androidx.room.ForeignKey.SET_NULL -> "SET NULL"
                androidx.room.ForeignKey.SET_DEFAULT -> "SET DEFAULT"
                androidx.room.ForeignKey.CASCADE -> "CASCADE"
                else -> throw UnsupportedOperationException("Invalid foreign key action")
            }
    }
}


