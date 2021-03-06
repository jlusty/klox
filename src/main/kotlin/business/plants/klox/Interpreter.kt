package business.plants.klox

import business.plants.klox.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    val globals: Environment = Environment()
    private var environment: Environment = globals
    private val locals: MutableMap<Expr, Int> = HashMap()

    init {
        globals.define("clock", object : LoxCallable {
            override fun arity(): Int {
                return 0
            }

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Double {
                return System.currentTimeMillis() / 1000.0
            }

            override fun toString(): String {
                return "<native fn>"
            }
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value: Any? = evaluate(expr.value)

        val distance: Int? = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
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

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee: Any? = evaluate(expr.callee)

        val arguments: MutableList<Any?> = ArrayList()
        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes")
        }

        val function: LoxCallable = callee
        if (arguments.size != function.arity()) {
            throw RuntimeError(expr.paren, "Expected ${function.arity()} arguments but got ${arguments.size}")
        }

        return function.call(this, arguments)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left: Any? = evaluate(expr.left)

        when (expr.operator.type) {
            OR -> if (isTruthy(left)) return left
            else -> if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
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

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance: Int? = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
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

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous: Environment = this.environment
        try {
            this.environment = environment

            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value: Any? = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        var value: Any? = null
        if (stmt.value != null) value = evaluate(stmt.value)

        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        var value: Any? = null
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
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