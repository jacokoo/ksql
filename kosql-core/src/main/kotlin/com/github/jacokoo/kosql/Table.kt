package com.github.jacokoo.kosql

import com.github.jacokoo.kosql.mapping.EmptyEntity
import com.github.jacokoo.kosql.mapping.Entity
import com.github.jacokoo.kosql.statements.QueryData
import com.github.jacokoo.kosql.statements.SQLBuilderContext
import kotlin.reflect.KClass

interface Nameable<out T>: SQLPart {
    val name: String
    val alias: String
    val aliasRequired: Boolean
    fun AS(alias: String): T

    fun sqlName(ctx: SQLBuilderContext): String = name
    override fun toSQL(ctx: SQLBuilderContext): String =
            ctx.alias(this)?.let { "${sqlName(ctx) } AS $it" } ?: sqlName(ctx)

}

interface Column<T>: Nameable<Column<T>> {
    val table: Table<*>
    val type: DataType<T>
    val allowNull: Boolean
    val defaultValue: T

    fun ASC(): Pair<Column<T>, Order> = this to Order.ASC
    fun DESC(): Pair<Column<T>, Order> = this to Order.DESC
}

data class DefaultColumn<T>(
        override val table: Table<*>,
        override val name: String,
        override val type: DataType<T>,
        override val alias: String = "",
        override val aliasRequired: Boolean = false,
        override val allowNull: Boolean = false,
        override val defaultValue: T = type.nullValue
): Column<T> {
    override fun AS(alias: String): Column<T> = this.copy(alias = alias)
    override fun sqlName(ctx: SQLBuilderContext): String = "${ctx.alias(table)}.$name"

    fun nullable(): DefaultColumn<T> = this.copy(allowNull = true)
    fun default(t: T): DefaultColumn<T> = this.copy(defaultValue = t)
}

abstract class Table<T>(override val name: String, override val alias: String = ""): Nameable<Table<T>> {
    override val aliasRequired = true
    var columns: List<Column<*>> = listOf()

    fun int(name: String) = DefaultColumn(this, name, IntType()).also { columns += it }
    fun float(name: String) = DefaultColumn(this, name, FloatType()).also { columns += it }
    fun decimal(name: String) = DefaultColumn(this, name, DecimalType()).also { columns += it }
    fun string(name: String) = DefaultColumn(this, name, StringType()).also { columns += it }

    open fun entityClass(): KClass<*> = EmptyEntity::class

    abstract fun create(): Entity<T, Table<T>>
    abstract fun primaryKey(): Column<T>
}

open class EmptyTable(alias: String = ""): Table<Any>(alias) {
    override fun AS(alias: String): EmptyTable {
        throw RuntimeException("never call this method")
    }
    override fun create(): Entity<Any, EmptyTable> {
        throw RuntimeException("never call this method")
    }

    override fun primaryKey(): Column<Any> {
        throw RuntimeException("never call this method")
    }
}

class TableLike(private val data: QueryData, alias: String, private val original: List<Column<*>>): EmptyTable(alias) {
    private val map = original.associate { it to DefaultColumn(this, if (it.alias == "") it.name else it.alias, it.type) }
    init {
        this.columns = map.values.toList()
    }

    override fun sqlName(ctx: SQLBuilderContext): String = "(${ctx.builder.build(data, ctx).sql})"

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(col: Column<T>): Column<T> {
        val c = original.find { it.table == col.table && it.name == col.name }
                ?: throw RuntimeException("Can not find column $col.name in table-like $alias")
        return map[c] as Column<T>
    }

    @Suppress("UNCHECKED_CAST")
    inline operator fun <reified T> get(name: String): Column<T> {
        return columns.find { it.name == name }!! as Column<T>
    }
}
