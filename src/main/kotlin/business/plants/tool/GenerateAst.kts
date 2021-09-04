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
        "Binary - left: Expr, operator: Token, right: Expr",
        "Grouping - expression: Expr",
        "Literal - value: Any",
        "Unary - operator: Token, right: Expr",
    )
)

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path: String = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")

    writer.println("package business.plants.klox")
    writer.println()
    writer.println("abstract class $baseName {")
    writer.println("    companion object {")

    // The AST classes
    for (type in types) {
        val className = type.split("-")[0].trim()
        val fields = type.split("-")[1].trim()
        defineType(writer, baseName, className, fields)
    }

    writer.println("    }")
    writer.println("}")
    writer.close()
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    writer.println("        class $className($fieldList) : $baseName() {")
    writer.println("        }")
}