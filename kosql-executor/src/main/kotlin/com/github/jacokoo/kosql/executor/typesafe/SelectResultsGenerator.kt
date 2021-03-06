package com.github.jacokoo.kosql.executor.typesafe

fun times(n: Int, b: (Int) -> String): String = (1..n).map { b(it) }.joinToString()

fun main(args: Array<String>) {
    val count = 10

    val title = """
        |package com.github.jacokoo.kosql.executor.typesafe
        |
        |import com.github.jacokoo.kosql.compose.statements.SelectStatement
        |import com.github.jacokoo.kosql.compose.typesafe.*
        |import com.github.jacokoo.kosql.executor.Query
        |import com.github.jacokoo.kosql.executor.ResultSetMapper
        |import com.github.jacokoo.kosql.executor.ResultSetRow
        |import com.github.jacokoo.kosql.executor.SelectResult
        |
    """.trimMargin()

    fun classPart(it: Int) = """
        |
        |class SelectResultMapper$it<${times(it) { "T$it" }}>(private val c: Column$it<${times(it) { "T$it" }}>): ResultSetMapper<Value$it<${times(it) { "T$it" }}>> {
        |    override fun map(rs: ResultSetRow) = Value$it(${times(it) { "rs[${it - 1}, c.c$it]" }})
        |}
        |data class SelectResult$it<${times(it) { "T$it" }}>(private val c: Column$it<${times(it) { "T$it" }}>, override val values: List<Value$it<${times(it) { "T$it" }}>>): SelectResult<Value$it<${times(it) { "T$it" }}>> {
        |    override val columns = c
        |    constructor(s: SelectStatement<Column$it<${times(it) { "T$it" }}>>, ko: Query): this(s.data.columns, ko.execute(s, SelectResultMapper$it(s.data.columns)))
        |}
    """.trimMargin()

    fun methodPart(it: Int) = """
        |
        |    fun <${times(it) { "T$it" }}> SelectStatement<Column$it<${times(it) { "T$it" }}>>.fetch() = SelectResult$it(this, this@Queries)
    """.trimMargin()

    println(buildString {
        append(title)
        (1..count).forEach { append(classPart(it)) }
        append("\n\ninterface Queries: Query {")
        append("\n    fun <T1> SelectStatement<Column1<T1>>.fetchValue(): T1 = fetch()[0].v1\n")
        (1..count).forEach { append(methodPart(it)) }
        append("\n}")
    })
}
