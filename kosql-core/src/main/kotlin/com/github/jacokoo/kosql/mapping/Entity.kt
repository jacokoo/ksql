package com.github.jacokoo.kosql.mapping

import com.github.jacokoo.kosql.EmptyTable
import com.github.jacokoo.kosql.Table

interface Entity<T: Any> {
    val TABLE: Table<T>
    operator fun set(name: String, value: Any)
    operator fun get(name: String): Any?
}

class EmptyEntity: Entity<Any> {
    override val TABLE: Table<Any> = EmptyTable()
    override fun get(name: String): Any? = null
    override fun set(name: String, value: Any) {}
}