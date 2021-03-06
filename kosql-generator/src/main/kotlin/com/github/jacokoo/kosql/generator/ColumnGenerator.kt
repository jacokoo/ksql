package com.github.jacokoo.kosql.generator

import com.github.jacokoo.kosql.compose.DataType
import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KClass

data class ColumnDefinition(
    val name: String,
    val dataType: Int,
    val typeName: String,
    val columnSize: Int,
    val decimalDigits: Int,
    val numPrecRadix: Int,
    val nullable: Boolean,
    val defaultValue: String?,
    val remark: String,
    val ordinalPosition: Int,
    val isAutoIncrement: Boolean,
    val isGeneratedColumn: Boolean
) {
    override fun toString(): String {
        return "$name $typeName($columnSize) $nullable :$remark"
    }

    companion object {
        fun fromResultSet(rs: ResultSet): ColumnDefinition = ColumnDefinition(
            rs.getString("COLUMN_NAME"),
            rs.getInt("DATA_TYPE"),
            rs.getString("TYPE_NAME"),
            rs.getInt("COLUMN_SIZE"),
            rs.getInt("DECIMAL_DIGITS"),
            rs.getInt("NUM_PREC_RADIX"),
            rs.getString("IS_NULLABLE").let { it != "NO" },
            rs.getString("COLUMN_DEF"),
            rs.getString("REMARKS"),
            rs.getInt("ORDINAL_POSITION"),
            rs.getString("IS_AUTOINCREMENT").let { it != "NO" },
            rs.getString("IS_GENERATEDCOLUMN").let { it != "NO" }
        )
    }
}

data class ColumnInfo(
        val name: String,
        val type: String, // the Datatype class for import
        val typeClass: KClass<*>,  // the kotlin class for field declaration
        val defaultValue: String,
        val define: String,
        val def: ColumnDefinition
)

interface ColumnGenerator {

    companion object {
        val ints = listOf(Types.INTEGER, Types.SMALLINT, Types.TINYINT)
        val bits = listOf(Types.BIT, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY)
        val strs = listOf(Types.CLOB, Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR,
                Types.LONGNVARCHAR, Types.NCHAR, Types.NVARCHAR)
    }

    /**
     * check if this generator support che column specification
     */
    fun support(tableName: String, def: ColumnDefinition): Boolean

    /**
     * generate column value
     */
    fun generete(tableName: String, def: ColumnDefinition, config: KoSQLGeneratorConfig): ColumnInfo

}

abstract class AbstractColumnGenerator<T>: ColumnGenerator {
    abstract val type: DataType<T>
    abstract val nullType: DataType<T>
    abstract fun kotlinType(): KClass<*>
    override fun generete(tableName: String, def: ColumnDefinition, config: KoSQLGeneratorConfig): ColumnInfo {
        val (define, dv) = createColumn(def)
        val name = if (def.nullable) nullType else type
        return ColumnInfo(config.namingStrategy.tableFieldName(def.name), name::class.java.name, kotlinType(), dv, define, def)
    }

    protected fun createColumn(def: ColumnDefinition): Pair<String, String> {
        val typeName = (if (def.nullable) nullType else type)::class.java.simpleName
        val defaultValue = def.defaultValue ?: type.nullValue
        val dv = parseDefaultValue(defaultValue)
        var str = "createColumn(\"${def.name}\", $typeName(), ${def.nullable}, $dv${if (def.isAutoIncrement) ", autoIncrement = true" else ""})"
        return  str to dv
    }

    protected open fun parseDefaultValue(v: Any?): String = v?.toString() ?: "null"
}
