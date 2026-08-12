package me.rerere.rikkahub.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle

internal enum class CodeTokenType {
    Plain,
    Comment,
    String,
    Keyword,
    Number,
    Function,
    Type,
    Annotation,
}

internal data class CodeToken(val text: String, val type: CodeTokenType)

private const val MAX_HIGHLIGHT_LENGTH = 50_000

private val keywordsByLanguage = mapOf(
    "kotlin" to "as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while by catch constructor delegate dynamic field file finally get import init param property receiver set setparam where actual abstract annotation companion const crossinline data enum expect external final infix inline inner internal lateinit noinline open operator out override private protected public reified sealed suspend tailrec vararg",
    "java" to "abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while true false null",
    "javascript" to "as async await break case catch class const continue debugger default delete do else export extends false finally for from function get if implements import in instanceof interface let new null of package private protected public return set static super switch this throw true try typeof undefined var void while with yield",
    "typescript" to "as async await break case catch class const continue declare debugger default delete do else enum export extends false finally for from function get if implements import in instanceof interface keyof let namespace never new null of package private protected public readonly return set static super switch this throw true try type typeof undefined var void while with yield",
    "python" to "and as assert async await break class continue def del elif else except false finally for from global if import in is lambda none nonlocal not or pass raise return true try while with yield",
    "sql" to "select from where join left right inner outer on as insert into update delete create alter drop table index view values set distinct group by order having limit offset union all case when then else end null true false primary key foreign references",
    "shell" to "if then else elif fi for while do done in case esac function return export local readonly unset source true false",
    "yaml" to "true false null yes no on off",
)

private val languageAliases = mapOf(
    "kt" to "kotlin", "kts" to "kotlin", "java" to "java",
    "js" to "javascript", "jsx" to "javascript", "mjs" to "javascript",
    "ts" to "typescript", "tsx" to "typescript",
    "py" to "python", "yml" to "yaml", "sh" to "shell", "bash" to "shell", "zsh" to "shell",
    "html" to "markup", "xml" to "markup", "svg" to "markup", "md" to "markup",
)

internal fun tokenizeCode(code: String, language: String): List<CodeToken> {
    if (code.length > MAX_HIGHLIGHT_LENGTH) return listOf(CodeToken(code, CodeTokenType.Plain))

    val normalizedLanguage = language.lowercase().trim().let { languageAliases[it] ?: it }
    val keywords = keywordsByLanguage[normalizedLanguage]?.split(' ')?.toSet().orEmpty()
    val isMarkup = normalizedLanguage == "markup"
    val isYaml = normalizedLanguage == "yaml"
    val tokens = mutableListOf<CodeToken>()
    var index = 0
    var plainStart = 0
    var yamlLineStart = 0

    fun add(start: Int, end: Int, type: CodeTokenType) {
        if (start < end) tokens += CodeToken(code.substring(start, end), type)
    }

    fun addHighlighted(end: Int, type: CodeTokenType) {
        add(plainStart, index, CodeTokenType.Plain)
        add(index, end, type)
        index = end
        plainStart = end
    }

    while (index < code.length) {
        if (isYaml && index == yamlLineStart && code[index] != '\n') {
            val lineEnd = code.indexOf('\n', index).let { if (it < 0) code.length else it }
            val colon = code.indexOf(':', index)
            if (colon in (index + 1)..lineEnd) {
                addHighlighted(colon, CodeTokenType.Annotation)
                continue
            }
        }
        val commentEnd = when {
            code.startsWith("//", index) -> code.indexOf('\n', index).let { if (it < 0) code.length else it }
            code[index] == '#' && (normalizedLanguage == "python" || normalizedLanguage == "shell" || isYaml) ->
                code.indexOf('\n', index).let { if (it < 0) code.length else it }
            code.startsWith("--", index) && normalizedLanguage == "sql" ->
                code.indexOf('\n', index).let { if (it < 0) code.length else it }
            code.startsWith("<!--", index) && isMarkup -> code.indexOf("-->", index + 4).let { if (it < 0) code.length else it + 3 }
            code.startsWith("/*", index) -> code.indexOf("*/", index + 2).let { if (it < 0) code.length else it + 2 }
            else -> index
        }
        if (commentEnd > index) {
            addHighlighted(commentEnd, CodeTokenType.Comment)
            continue
        }

        val character = code[index]
        if (character in "\"'`") {
            var end = index + 1
            while (end < code.length) {
                if (code[end] == '\\') end++
                else if (code[end] == character) {
                    end++
                    break
                }
                end++
            }
            addHighlighted(end.coerceAtMost(code.length), CodeTokenType.String)
            continue
        }

        if (character == '@' && (normalizedLanguage == "kotlin" || normalizedLanguage == "java") &&
            index + 1 < code.length && code[index + 1].isIdentifierStart()
        ) {
            var end = index + 2
            while (end < code.length && code[end].isIdentifierPart()) end++
            addHighlighted(end, CodeTokenType.Annotation)
            continue
        }

        val numberEnd = code.numberEndAt(index)
        if (numberEnd > index) {
            addHighlighted(numberEnd, CodeTokenType.Number)
            continue
        }

        if (character.isIdentifierStart()) {
            var end = index + 1
            while (end < code.length && code[end].isIdentifierPart()) end++
            val type = when {
                code.substring(index, end).lowercase() in keywords -> CodeTokenType.Keyword
                code.indexOfFirstNonWhitespace(end).let { it < code.length && code[it] == '(' } -> CodeTokenType.Function
                character.isUpperCase() -> CodeTokenType.Type
                else -> CodeTokenType.Plain
            }
            if (type != CodeTokenType.Plain) addHighlighted(end, type) else index = end
            continue
        }

        if (isMarkup && character in "</>") {
            var end = index + 1
            while (end < code.length && !code[end].isWhitespace() && code[end] != '>') end++
            addHighlighted(end, CodeTokenType.Keyword)
            continue
        }

        if (character == '\n') yamlLineStart = index + 1
        index++
    }
    add(plainStart, code.length, CodeTokenType.Plain)
    return tokens
}

private fun Char.isIdentifierStart(): Boolean = this in 'A'..'Z' || this in 'a'..'z' || this == '_'

private fun Char.isIdentifierPart(): Boolean = isIdentifierStart() || this in '0'..'9'

private fun String.indexOfFirstNonWhitespace(start: Int): Int {
    var index = start
    while (index < length && this[index].isWhitespace()) index++
    return index
}

private fun String.numberEndAt(start: Int): Int {
    if (start >= length || this[start] !in '0'..'9') return start
    var index = start
    if (this[start] == '0' && start + 2 <= length) {
        val radix = this.getOrNull(start + 1)
        val validDigit: (Char) -> Boolean = when (radix) {
            'x', 'X' -> { character -> character in '0'..'9' || character in 'a'..'f' || character in 'A'..'F' || character == '_' }
            'b', 'B' -> { character -> character == '0' || character == '1' || character == '_' }
            else -> return decimalNumberEndAt(start)
        }
        index += 2
        while (index < length && validDigit(this[index])) index++
        return if (index == start + 2) start else index
    }
    return decimalNumberEndAt(start)
}

private fun String.decimalNumberEndAt(start: Int): Int {
    var index = start
    while (index < length && (this[index] in '0'..'9' || this[index] == '_')) index++
    if (index < length && this[index] == '.') {
        index++
        while (index < length && (this[index] in '0'..'9' || this[index] == '_')) index++
    }
    if (index < length && this[index] in "eE") {
        val exponentStart = index++
        if (index < length && this[index] in "+-") index++
        val digitsStart = index
        while (index < length && this[index] in '0'..'9') index++
        if (index == digitsStart) index = exponentStart
    }
    return index
}

@Composable
internal fun highlightedCode(code: String, language: String): AnnotatedString {
    val colorScheme = MaterialTheme.colorScheme
    val colors = DesktopCodeColors(
        comment = colorScheme.onSurfaceVariant,
        string = colorScheme.tertiary,
        keyword = colorScheme.primary,
        number = colorScheme.secondary,
        function = colorScheme.error,
        typeColor = colorScheme.secondary,
        annotation = colorScheme.tertiary,
    )
    val tokens = remember(code, language) { tokenizeCode(code, language) }
    return buildAnnotatedString {
        tokens.forEach { token ->
            val style = colors.styleFor(token.type)
            if (style == null) append(token.text) else withStyle(style) { append(token.text) }
        }
    }
}

private data class DesktopCodeColors(
    val comment: Color,
    val string: Color,
    val keyword: Color,
    val number: Color,
    val function: Color,
    val typeColor: Color,
    val annotation: Color,
) {
    fun styleFor(type: CodeTokenType): SpanStyle? = when (type) {
        CodeTokenType.Plain -> null
        CodeTokenType.Comment -> SpanStyle(color = comment, fontStyle = FontStyle.Italic)
        CodeTokenType.String -> SpanStyle(color = string)
        CodeTokenType.Keyword -> SpanStyle(color = keyword)
        CodeTokenType.Number -> SpanStyle(color = number)
        CodeTokenType.Function -> SpanStyle(color = function)
        CodeTokenType.Type -> SpanStyle(color = typeColor)
        CodeTokenType.Annotation -> SpanStyle(color = annotation)
    }
}
