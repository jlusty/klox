package business.plants.tool

import java.io.PrintWriter
import kotlin.system.exitProcess

if (args.size != 1) {
    println("Usage: generate_ast <output directory>")
    exitProcess(64)
}
val outputDir: String = args[0]

defineAst(
    outputDir, "Expr", listOf(
        "Assign - name: Token, value: Expr",
        "Binary - left: Expr, operator: Token, right: Expr",
        "Call - callee: Expr, paren: Token, arguments: List<Expr>",
        "Grouping - expression: Expr",
        "Literal - value: Any?",
        "Logical - left: Expr, operator: Token, right: Expr",
        "Unary - operator: Token, right: Expr",
        "Variable - name: Token",
    )
)

defineAst(
    outputDir, "Stmt", listOf(
        "Block - statements: List<Stmt>",
        "Expression - expression: Expr",
        "If - condition: Expr, thenBranch: Stmt, elseBranch: Stmt?",
        "Print - expression: Expr",
        "Var - name: Token, initializer: Expr?",
        "While - condition: Expr, body: Stmt",
    )
)

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path: String = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")

    writer.println("// Generated code: produced by GenerateAst")
    writer.println()
    writer.println("package business.plants.klox")
    writer.println()
    writer.println("abstract class $baseName {")

    defineVisitor(writer, baseName, types)

    // The AST classes
    for (type in types) {
        val className = type.split("-")[0].trim()
        val fields = type.split("-")[1].trim()
        defineType(writer, baseName, className, fields)
    }

    // The base accept() method
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")

    writer.println("}")
    writer.close()
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.println("    interface Visitor<R> {")

    for (type in types) {
        val typeName: String = type.split("-")[0].trim()
        writer.println("        fun visit$typeName$baseName(${baseName.lowercase()}: $typeName): R")
    }

    writer.println("    }")
    writer.println()
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    val fieldListVals = (fieldList.split(", ").map { field -> "val $field" }).joinToString(", ")
    writer.println("    class $className($fieldListVals) : $baseName() {")

    // Visitor pattern
    writer.println("        override fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("            return visitor.visit$className$baseName(this)")
    writer.println("        }")

    writer.println("    }")
    writer.println()
}