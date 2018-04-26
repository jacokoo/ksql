package com.github.jacokoo.kosql.mapping

import com.github.jacokoo.kosql.Column
import com.github.jacokoo.kosql.statements.ColumnList
import com.github.jacokoo.kosql.statements.QueryPart
import com.github.jacokoo.kosql.statements.SelectStatement
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

interface ResultRow {
    val values: List<Any?>
    operator fun get(idx: Int): Any? = values[idx]
}

internal class ResultIterator<T: ResultRow>(private val t: QueryResult<T>): AbstractIterator<T>() {
    private var current = 0

    override fun computeNext() {
        if (current >= t.size()) done()
        else setNext(t.values[current++])
    }
}

data class ResultSetRow(private val rs: ResultSet) {
    operator fun <T> get(idx: Int, col: Column<T>): T = col.type.fromDb(rs.getObject(idx + 1))
    operator fun <T> get(col: Column<T>): T = col.type.fromDb(rs.getObject(col.name))

    operator fun get(idx: Int): Any? = rs.getObject(idx + 1)
    operator fun get(name: String): Any? = rs.getObject(name)
}

interface ResultSetMapper<out R> {
    fun map(rs: ResultSetRow): R
}

class ColumnsToEntityMapper<R: Entity<*, *>>(val columns: ColumnList, val entityClass: KClass<R>): ResultSetMapper<R> {
    @Suppress("UNCHECKED_CAST")
    override fun map(rs: ResultSetRow): R {
        val cs = columns.columns.filter { Database[it.table] == entityClass }
        if (cs.none()) throw RuntimeException("no columns for entity")
        val clazz = Database[cs[0].table] ?: throw RuntimeException("no entity class found")
        return clazz.createInstance().also {
            columns.columns.forEach {c -> if (cs.contains(c)) it[c.name] = rs[c]}
        } as R
    }
}

interface QueryResult<out T: ResultRow>: Iterable<T> {
    val columns: ColumnList
    val values: List<T>

    @Suppress("UNCHECKED_CAST")
    fun <T: Entity<*, *>> into(entityClass: KClass<T>): List<T> {
        val cs = columns.columns.filter { Database[it.table] == entityClass }
        if (cs.none()) return listOf()

        return values.map { v -> entityClass.createInstance().also { e ->
            columns.columns.forEachIndexed {i, c -> if (cs.contains(c)) e[c.name] = v[i]}
        } }
    }

    fun size() = values.size
    fun isEmpty() = values.isEmpty()
    operator fun get(idx: Int) = values[idx]

    override fun iterator(): Iterator<T> = ResultIterator(this)
}

data class ResultRows(override val values: List<Any?>): ResultRow
data class QueryResults(override val columns: ColumnList, override val values: List<ResultRows>): QueryResult<ResultRows> {
    constructor(cs: ColumnList, qp: QueryPart, ko: QueryResultExtension): this(cs, ko.execute(qp, Mapper(cs)))
    private class Mapper(private val cs: ColumnList): ResultSetMapper<ResultRows> {
        override fun map(rs: ResultSetRow) = ResultRows(cs.columns.mapIndexed {idx, col -> rs[idx, col]})
    }
}

interface QueryResultExtension {
    fun <T> execute(qp: QueryPart, mapper: ResultSetMapper<T>): List<T>
    fun <T> QueryPart.fetch(mapper: ResultSetMapper<T>): List<T> = execute(this, mapper)
    fun <T: Entity<*, *>> QueryPart.fetch(entityClass: KClass<T>) = fetch(ColumnsToEntityMapper(this.data.columns, entityClass))
    fun <T> QueryPart.fetch(mapper: (ResultSetRow) -> T): List<T> = fetch(object: ResultSetMapper<T>{
        override fun map(rs: ResultSetRow): T = mapper(rs)
    })
    fun SelectStatement.fetch(): QueryResults = QueryResults(this.data.columns, this, this@QueryResultExtension)

}


