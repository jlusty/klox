package business.plants.klox

import business.plants.klox.TokenType.*

class Parser(private val tokens: List<Token>) {
    private class ParseError : RuntimeException() {}

    private var current: Int = 0;

    fun parse(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()
        while (!isAtEnd()) {
            val statement = declaration()
            if (statement != null) {
                statements.add(statement)
            }
        }

        return statements
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun declaration(): Stmt? {
        try {
            if (match(VAR)) return varDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun statement(): Stmt {
        if (match(IF)) return ifStatement()
        if (match(PRINT)) return printStatement()
        if (match(LEFT_BRACE)) return Stmt.Block(block())

        return expressionStatement()
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'")
        val condition: Expr = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition")

        val thenBranch: Stmt = statement()
        var elseBranch: Stmt? = null
        if (match(ELSE)) {
            elseBranch = statement()
        }

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value: Expr = expression()
        consume(SEMICOLON, "Expect ';' after value")
        return Stmt.Print(value)
    }

    private fun varDeclaration(): Stmt {
        val name: Token = consume(IDENTIFIER, "Expect variable name")

        var initializer: Expr? = null;
        if (match(EQUAL)) {
            initializer = expression()
        }

        consume(SEMICOLON, "Expect ';' after variable declaration")
        return Stmt.Var(name, initializer)
    }

    private fun expressionStatement(): Stmt {
        val expr: Expr = expression()
        consume(SEMICOLON, "Expect ';' after value")
        return Stmt.Expression(expr)
    }

    private fun block(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            val statement = declaration()
            if (statement != null) {
                statements.add(statement)
            }
        }

        consume(RIGHT_BRACE, "Expect '}' after block")
        return statements
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(EQUAL)) {
            val equals: Token = previous()
            val value: Expr = assignment()

            if (expr is Expr.Variable) {
                val name: Token = expr.name
                return Expr.Assign(name, value)
            }

            error(equals, "Invalid assignment target")
        }

        return expr
    }

    private fun or(): Expr {
        var expr: Expr = and()
        while (match(OR)) {
            val operator: Token = previous()
            val right: Expr = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr: Expr = equality()
        while (match(AND)) {
            val operator: Token = previous()
            val right: Expr = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr: Expr = comparison()
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator: Token = previous()
            val right: Expr = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr: Expr = term()
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator: Token = previous()
            val right: Expr = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr: Expr = factor()
        while (match(MINUS, PLUS)) {
            val operator: Token = previous()
            val right: Expr = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr: Expr = unary()
        while (match(SLASH, STAR)) {
            val operator: Token = previous()
            val right: Expr = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator: Token = previous()
            val right: Expr = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {
        return when {
            match(FALSE) -> Expr.Literal(false)
            match(TRUE) -> Expr.Literal(true)
            match(NIL) -> Expr.Literal(null)
            match(NUMBER, STRING) -> Expr.Literal(previous().literal)
            match(IDENTIFIER) -> Expr.Variable(previous())
            match(LEFT_PAREN) -> {
                val expr: Expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression")
                return Expr.Grouping(expr)
            }
            else -> throw error(peek(), "Expect expression")
        }
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()

        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        Klox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return

            when (peek().type) {
                CLASS, FOR, FUN, IF, PRINT, RETURN, VAR, WHILE -> return
            }

            advance()
        }
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }
}