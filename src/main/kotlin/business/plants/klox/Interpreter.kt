package business.plants.klox

import business.plants.klox.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG_EQUAL ->
                !isEqual(left, right)
            EQUAL_EQUAL ->
                isEqual(left, right)
            GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) > (right as Double)
            }
            GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) >= (right as Double)
            }
            LESS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) < (right as Double)
            }
            LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) <= (right as Double)
            }
            MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) - (right as Double)
            }
            PLUS -> {
                return when {
                    left is Double && right is Double -> left + right
                    left is String && right is String -> left + right
                    else -> throw RuntimeError(expr.operator, "Operands must be two numbers or two strings")
                }
            }
            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) / (right as Double)
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) + (right as Double)
            }
            // Unreachable
            else -> null
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right: Any? = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG -> !isTruthy(right)
            MINUS -> {
                // TODO: Refactor to remove assertion?
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            // Unreachable
            else -> null
        }
    }

    private fun isTruthy(any: Any?): Boolean {
        return when (any) {
            null -> false
            is Boolean -> any
            else -> return true
        }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        return a == b
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers")
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value: Any? = evaluate(stmt.expression)
        println(stringify(value))
    }

    private fun stringify(any: Any?): String {
        return when (any) {
            null -> "nil"
            is Double -> {
                var text: String = any.toString()
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length - 2)
                }
                return text
            }
            else -> any.toString()
        }
    }
}