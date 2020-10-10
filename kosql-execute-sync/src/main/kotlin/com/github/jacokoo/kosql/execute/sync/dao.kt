package com.github.jacokoo.kosql.execute.sync

import com.github.jacokoo.kosql.compose.Column
import com.github.jacokoo.kosql.compose.Entity
import com.github.jacokoo.kosql.compose.Order
import com.github.jacokoo.kosql.compose.Table
import com.github.jacokoo.kosql.compose.expression.Count
import com.github.jacokoo.kosql.compose.expression.Exp
import com.github.jacokoo.kosql.compose.expression.Operator
import com.github.jacokoo.kosql.compose.statement.*
import com.github.jacokoo.kosql.compose.typesafe.Column1

interface Dao: Executor, Operator {
    fun <T> Entity<T>.save(): T? {
        val key = INNER_TABLE.primaryKey()
        var columns = INNER_TABLE.columns
        if (key.autoIncrement && this[key.name] == key.type.nullValue) {
            columns = columns.filter { it != key }
        }

        val values = Values(columns.map { this[it.name] })
        val (id, rows) = execute(InsertEnd(InsertData(INNER_TABLE, Columns(columns), listOf(values))))
        if (rows != 1) return null

        if (key.autoIncrement) this[key.name] = id
        return id
    }

    fun <T, E: Entity<T>, R: Table<T, E>> R.byId(t: T): E?
        = execute(SelectEnd(SelectData(Columns(inner.columns), this, expression = primaryKey() EQ t)), this).firstOrNull()

    fun <T, R: Table<T, Entity<T>>> R.count(exp: Exp<*>? = null): Int
        = SelectEnd(SelectData(Column1(Count<Any>()), this, expression = exp)).fetchValue()

    fun <T, E: Entity<T>, R: Table<T, E>> R.fetch(exp: Exp<*>? = null, vararg orders: Pair<Column<*>, Order>): List<E>
        = execute(SelectEnd(SelectData(Columns(inner.columns), this, expression = exp, orderBy = orders.toList())), this)

    fun <T, R: Table<T, Entity<T>>> R.delete(t: T): Boolean =
        execute(DeleteEnd(DeleteData(deletes = listOf(this), expression = primaryKey() EQ t))) == 1

    fun <T, R: Table<T, Entity<T>>> R.delete(exp: Exp<*>? = null): Int =
        execute(DeleteEnd(DeleteData(deletes = listOf(this), expression = exp)))

}