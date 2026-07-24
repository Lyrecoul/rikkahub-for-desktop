package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Desktop counterpart of the Android JSON expression evaluator, used by balance queries. */
internal fun evaluateDesktopJsonExpression(input: kotlin.String, root: JsonObject): kotlin.String {
    val value = DesktopJsonExpressionParser(input).parse().evaluate(root)
    return value.asString()
}

private sealed interface DesktopJsonExpression {
    fun evaluate(root: JsonObject): DesktopJsonValue
}

private data class DesktopNumber(val value: Double) : DesktopJsonExpression {
    override fun evaluate(root: JsonObject) = DesktopJsonValue.Number(value)
}

private data class DesktopString(val value: String) : DesktopJsonExpression {
    override fun evaluate(root: JsonObject) = DesktopJsonValue.String(value)
}

private data class DesktopPath(val parts: List<DesktopPathPart>) : DesktopJsonExpression {
    override fun evaluate(root: JsonObject): DesktopJsonValue {
        var element: JsonElement = root
        parts.forEach { part ->
            element = when (part) {
                is DesktopPathPart.Field -> (element as? JsonObject)?.get(part.name) ?: return DesktopJsonValue.String("")
                is DesktopPathPart.Index -> (element as? JsonArray)?.getOrNull(part.index) ?: return DesktopJsonValue.String("")
            }
        }
        return element.toValue()
    }
}

private data class DesktopUnary(val operator: Char, val expression: DesktopJsonExpression) : DesktopJsonExpression {
    override fun evaluate(root: JsonObject): DesktopJsonValue {
        val value = expression.evaluate(root).asNumber()
        return DesktopJsonValue.Number(if (operator == '-') -value else value)
    }
}

private data class DesktopBinary(
    val left: DesktopJsonExpression,
    val operator: kotlin.String,
    val right: DesktopJsonExpression
) : DesktopJsonExpression {
    override fun evaluate(root: JsonObject): DesktopJsonValue {
        val lhs = left.evaluate(root)
        val rhs = right.evaluate(root)
        return when (operator) {
            "++" -> DesktopJsonValue.String(lhs.asString() + rhs.asString())
            "+" -> DesktopJsonValue.Number(lhs.asNumber() + rhs.asNumber())
            "-" -> DesktopJsonValue.Number(lhs.asNumber() - rhs.asNumber())
            "*", "x", "X" -> DesktopJsonValue.Number(lhs.asNumber() * rhs.asNumber())
            "/" -> DesktopJsonValue.Number(lhs.asNumber() / rhs.asNumber())
            else -> error("不支持的余额表达式运算符：$operator")
        }
    }
}

private sealed interface DesktopPathPart {
    data class Field(val name: String) : DesktopPathPart
    data class Index(val index: Int) : DesktopPathPart
}

private sealed interface DesktopJsonValue {
    fun asString(): kotlin.String
    fun asNumber(): Double

    data class String(val value: kotlin.String) : DesktopJsonValue {
        override fun asString(): kotlin.String = value
        override fun asNumber() = value.toDoubleOrNull() ?: 0.0
    }

    data class Number(val value: Double) : DesktopJsonValue {
        override fun asString(): kotlin.String = if (value.isFinite() && value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
        override fun asNumber() = value
    }
}

private fun JsonElement.toValue(): DesktopJsonValue = when (this) {
    JsonNull -> DesktopJsonValue.String("")
    is JsonPrimitive -> content.toDoubleOrNull()?.let(DesktopJsonValue::Number) ?: DesktopJsonValue.String(content)
    else -> DesktopJsonValue.String(toString())
}

private class DesktopJsonExpressionParser(private val source: String) {
    private var position = 0

    fun parse(): DesktopJsonExpression {
        val expression = parseConcat()
        skipWhitespace()
        require(position == source.length) { "余额表达式第 ${position + 1} 个字符无效" }
        return expression
    }

    private fun parseConcat(): DesktopJsonExpression {
        var expression = parseAdditive()
        while (consume("++")) expression = DesktopBinary(expression, "++", parseAdditive())
        return expression
    }

    private fun parseAdditive(): DesktopJsonExpression {
        var expression = parseMultiplicative()
        while (true) {
            expression = when {
                consumeSinglePlus() -> DesktopBinary(expression, "+", parseMultiplicative())
                consume("-") -> DesktopBinary(expression, "-", parseMultiplicative())
                else -> return expression
            }
        }
    }

    private fun parseMultiplicative(): DesktopJsonExpression {
        var expression = parseUnary()
        while (true) {
            expression = when {
                consume("*") -> DesktopBinary(expression, "*", parseUnary())
                consume("x") -> DesktopBinary(expression, "x", parseUnary())
                consume("X") -> DesktopBinary(expression, "X", parseUnary())
                consume("/") -> DesktopBinary(expression, "/", parseUnary())
                else -> return expression
            }
        }
    }

    private fun parseUnary(): DesktopJsonExpression = when {
        consume("+") -> DesktopUnary('+', parseUnary())
        consume("-") -> DesktopUnary('-', parseUnary())
        else -> parsePrimary()
    }

    private fun parsePrimary(): DesktopJsonExpression {
        skipWhitespace()
        return when {
            consume("(") -> parseConcat().also { require(consume(")")) { "余额表达式缺少右括号" } }
            peek() == '"' -> DesktopString(parseString())
            peek()?.isDigit() == true -> DesktopNumber(parseNumber())
            peek()?.let(::isIdentifierStart) == true -> parsePath()
            else -> error("余额表达式第 ${position + 1} 个字符无效")
        }
    }

    private fun parsePath(): DesktopJsonExpression {
        val parts = mutableListOf<DesktopPathPart>(DesktopPathPart.Field(parseIdentifier()))
        while (true) {
            when {
                consume(".") -> parts += DesktopPathPart.Field(parseIdentifier())
                consume("[") -> {
                    skipWhitespace()
                    val start = position
                    while (peek()?.isDigit() == true) position++
                    require(start != position && consume("]")) { "余额表达式数组下标无效" }
                    parts += DesktopPathPart.Index(source.substring(start, position - 1).toInt())
                }
                else -> return DesktopPath(parts)
            }
        }
    }

    private fun parseIdentifier(): String {
        skipWhitespace()
        val start = position
        require(peek()?.let(::isIdentifierStart) == true) { "余额表达式字段名无效" }
        position++
        while (peek()?.let(::isIdentifierPart) == true) position++
        return source.substring(start, position)
    }

    private fun parseNumber(): Double {
        val start = position
        while (peek()?.isDigit() == true) position++
        if (peek() == '.') {
            position++
            while (peek()?.isDigit() == true) position++
        }
        return source.substring(start, position).toDouble()
    }

    private fun parseString(): String {
        require(consume("\""))
        val value = StringBuilder()
        while (position < source.length && peek() != '"') {
            val character = source[position++]
            if (character == '\\' && position < source.length) {
                value.append(when (val escaped = source[position++]) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    else -> escaped
                })
            } else value.append(character)
        }
        require(consume("\"")) { "余额表达式字符串未闭合" }
        return value.toString()
    }

    private fun consume(token: String): Boolean {
        skipWhitespace()
        if (!source.startsWith(token, position)) return false
        position += token.length
        return true
    }

    private fun consumeSinglePlus(): Boolean {
        skipWhitespace()
        if (!source.startsWith("+", position) || source.startsWith("++", position)) return false
        position++
        return true
    }

    private fun skipWhitespace() {
        while (peek()?.isWhitespace() == true) position++
    }

    private fun peek(): Char? = source.getOrNull(position)
    private fun isIdentifierStart(character: Char) = character == '_' || character.isLetter()
    private fun isIdentifierPart(character: Char) = isIdentifierStart(character) || character.isDigit()
}
