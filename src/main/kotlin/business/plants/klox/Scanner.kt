package business.plants.klox

import business.plants.klox.TokenType.*

class Scanner(private val source: String) {
    private val keywords: Map<String, TokenType> = hashMapOf(
        "and" to AND,
        "class" to CLASS,
        "else" to ELSE,
        "false" to FALSE,
        "for" to FOR,
        "fun" to FUN,
        "if" to IF,
        "nil" to NIL,
        "or" to OR,
        "print" to PRINT,
        "return" to RETURN,
        "super" to SUPER,
        "this" to THIS,
        "true" to TRUE,
        "var" to VAR,
        "while" to WHILE
    )

    private val tokens: MutableList<Token> = ArrayList()
    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
            '/' -> if (match('/')) {
                // A comment goes until the end of the line
                while (peek() != '\n' && !isAtEnd()) advance()
            } else {
                addToken(SLASH)
            }
            // Ignore whitespace
            ' ', '\r', '\t' -> Unit
            '\n' -> line++
            '"' -> string()
            else -> {
                when {
                    isDigit(c) -> number()
                    isAlpha(c) -> identifier()
                    else -> error(line, "Unexpected character")
                }
            }
        }
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()

        val text: String = source.substring(start, current)
        addToken(keywords[text] ?: IDENTIFIER)
    }

    private fun number() {
        while (isDigit(peek())) advance()

        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance()

            while (isDigit(peek())) advance()
        }

        addToken(NUMBER, source.substring(start, current).toDouble())
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            error(line, "Unterminated string")
            return
        }

        advance() // The closing "

        // Trim the surrounding quotes
        val value: String = source.substring(start + 1, current - 1)
        addToken(STRING, value)
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return source[current]
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length) return '\u0000'
        return source[current + 1]
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun isAlpha(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_'
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun addToken(type: TokenType) {
        addToken(type, null)
    }

    private fun addToken(type: TokenType, literal: Any?) {
        val text: String = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }
}