package com.github.jacokoo.kosql.generator

import java.io.Writer

class TableWriterFactory: ClassWriterFactory {
    override fun create(writer: Writer, config: KoSQLGeneratorConfig, table: TableInfo): ClassWriter {
        return TableWriter(writer, config, table)
    }
}

open class TableWriter(writer: Writer, val config: KoSQLGeneratorConfig, val table: TableInfo): AbstractClassWriter(writer) {
    override fun writePackage() {
        writer.write("package ${config.outputPackage}.kosql.table\n")
    }

    override fun writeImports() {
        table.imports.forEach { writer.write("import $it\n") }
    }

    override fun writeSignature() {
        val pk = table.primaryKey
        writer.write("open class ${table.tableName} protected constructor(alias: String = \"\"):" +
                " Table<${pk.typeClass.simpleName}>(\"${table.def.name}\", alias, \"\")")
    }

    override fun writeFields() {
        table.columns.forEach {
            writer.write("    val ${it.name} = ${it.define}\n")
        }
    }

    override fun writeMethods() {
        writer.write("    override fun AS(alias: String) = ${table.tableName}(alias)\n")
        writer.write("    override fun primaryKey() = ${table.primaryKey.name}\n")
        writer.write("    operator fun unaryMinus() = Column")
        writer.write(if (table.columns.size > 22) "s" else "${table.columns.size}(")
        writer.write(table.columns.map { it.name }.joinToString())
        writer.write(")\n")
    }

    override fun writeTail() {
        writer.write("object ${table.objectName}: ${table.tableName}()\n")
    }
}