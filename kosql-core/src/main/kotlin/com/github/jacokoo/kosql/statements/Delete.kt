package com.github.jacokoo.kosql.statements

import com.github.jacokoo.kosql.Statement
import com.github.jacokoo.kosql.Table

data class DeleteData(
        val deletes: List<Table<*>> = listOf(),
        val table: Table<*>? = null,
        val joins: List<Join> = listOf(),
        val expression: Expression<*>? = null
): WhereData<DeleteData>, JoinData<DeleteData> {
    override fun getWhere() = expression
    override fun setWhere(e: Expression<*>) = copy(expression = e)
    override fun addJoin(join: Join) = copy(joins = joins + join)
    override fun removeJoin(join: Join) = copy(joins = joins - join)
}

interface DeleteStatement: Statement {
    val data: DeleteData
}

data class DeleteWhereDataContainer(override val data: DeleteData): AbstractWhereDataContainer<DeleteData, DeleteWhereDataContainer>() {
    override fun refer(data: DeleteData) = DeleteWhereDataContainer(data)
}

interface DeleteWhereOperate: WhereOperate<DeleteData, DeleteWhereDataContainer> {
    override fun refer(data: DeleteData) = DeleteWhereDataContainer(data)
}

data class DeleteWherePart(override val data: DeleteData): DeleteWhereOperate, DeleteStatement

data class DeleteJoinDataContainer(override val data: DeleteData, override val join: Join): AbstractJoinDataContainer<DeleteData, DeleteJoinDataContainer>() {
    override fun refer() = this
    override fun refer(data: DeleteData, join: Join) = DeleteJoinDataContainer(data, join)
}

data class DeleteJoinOnPart(override val data: DeleteData, override val join: Join): JoinOnPart<DeleteData, DeleteJoinDataContainer>, DeleteStatement {
    override fun refer(data: DeleteData, join: Join) = DeleteJoinDataContainer(data, join)
}

interface DeleteJoinOperate: JoinOperate<DeleteData, DeleteJoinDataContainer, DeleteJoinOnPart> {
    override fun referJoinOn(data: DeleteData, join: Join) = DeleteJoinOnPart(data, join)
}

data class DeleteJoinPart(override val data: DeleteData): DeleteJoinOperate, DeleteWhereOperate, DeleteStatement

data class DeleteFromPart(override val data: DeleteData): DeleteStatement {
    infix fun FROM(table: Table<*>): DeleteJoinPart = DeleteJoinPart(data.copy(table = table))
}

interface Delete {
    object DELETE {
        operator fun invoke(vararg ts: Table<*>): DeleteFromPart = DeleteFromPart(DeleteData(deletes = ts.toList()))
        infix fun FROM(t: Table<*>): DeleteWherePart = DeleteWherePart(DeleteData(deletes = listOf(t)))
    }
}