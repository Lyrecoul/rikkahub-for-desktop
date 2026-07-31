package me.rerere.rikkahub.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

private val identifier = Regex("[A-Za-z_][A-Za-z0-9_]*")
private val number = Regex("(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.[\\d_]+)?(?:[eE][+-]?\\d+)?)")
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
    val keywords = keywordsByLanguage[normalizedLanguage]
        ?.split(' ')
        ?.map { it.lowercase() }
        ?.toSet()
        .orEmpty()
    val tokens = mutableListOf<CodeToken>()
    var index = 0

    fun add(text: String, type: CodeTokenType) {
        if (text.isNotEmpty()) tokens += CodeToken(text, type)
    }

    while (index < code.length) {
        val remaining = code.substring(index)
        val isMarkup = normalizedLanguage == "markup"
        val isYaml = normalizedLanguage == "yaml"
        val lineComment = when {
            remaining.startsWith("//") -> remaining.indexOf('\n').let { if (it < 0) remaining.length else it }
            remaining.startsWith('#') && (normalizedLanguage == "python" || normalizedLanguage == "shell" || isYaml) -> remaining.indexOf(
                '\n'
            ).let { if (it < 0) remaining.length else it }

            remaining.startsWith("--") && normalizedLanguage == "sql" -> remaining.indexOf('\n')
                .let { if (it < 0) remaining.length else it }

            remaining.startsWith("<!--") && isMarkup -> remaining.indexOf("-->")
                .let { if (it < 0) remaining.length else it + 3 }

            else -> 0
        }
        if (lineComment > 0) {
            add(remaining.take(lineComment), CodeTokenType.Comment)
            index += lineComment
            continue
        }
        if (remaining.startsWith("/*")) {
            val end = remaining.indexOf("*/").let { if (it < 0) remaining.length else it + 2 }
            add(remaining.take(end), CodeTokenType.Comment)
            index += end
            continue
        }
        if (remaining.first() in "\"'`") {
            val quote = remaining.first()
            var end = 1
            while (end < remaining.length) {
                if (remaining[end] == '\\') end++
                else if (remaining[end] == quote) {
                    end++
                    break
                }
                end++
            }
            add(remaining.take(end), CodeTokenType.String)
            index += end
            continue
        }
        if (remaining.first() == '@' && (normalizedLanguage == "kotlin" || normalizedLanguage == "java")) {
            val match = identifier.find(remaining, 1)
            if (match?.range?.first == 1) {
                add(remaining.substring(0, match.range.last + 1), CodeTokenType.Annotation)
                index += match.range.last + 1
                continue
            }
        }
        val numeric = number.find(remaining)
        if (numeric?.range?.first == 0) {
            add(numeric.value, CodeTokenType.Number)
            index += numeric.value.length
            continue
        }
        val word = identifier.find(remaining)
        if (word?.range?.first == 0) {
            val value = word.value
            val type = when {
                value.lowercase() in keywords -> CodeTokenType.Keyword
                remaining.drop(value.length).trimStart().startsWith("(") -> CodeTokenType.Function
                value.first().isUpperCase() -> CodeTokenType.Type
                else -> CodeTokenType.Plain
            }
            add(value, type)
            index += value.length
            continue
        }
        if (isMarkup && remaining.first() in "</>") {
            val end = remaining.indexOfFirst { it.isWhitespace() || it == '>' }.let { if (it < 0) 1 else it }
            add(remaining.take(end), CodeTokenType.Keyword)
            index += end
            continue
        }
        if (isYaml && remaining.first() != '\n') {
            val colon = remaining.indexOf(':')
            if (colon in 1..remaining.indexOf('\n').let { if (it < 0) remaining.length else it }) {
                add(remaining.take(colon), CodeTokenType.Annotation)
                index += colon
                continue
            }
        }
        add(remaining.take(1), CodeTokenType.Plain)
        index++
    }
    return tokens
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
    return buildAnnotatedString {
        tokenizeCode(code, language).forEach { token ->
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
