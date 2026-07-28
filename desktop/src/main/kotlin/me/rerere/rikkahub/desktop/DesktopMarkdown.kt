package me.rerere.rikkahub.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import dev.darkokoa.pangu.spacingText
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import java.awt.image.BufferedImage
import java.awt.datatransfer.StringSelection
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal data class MarkdownSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strikethrough: Boolean = false,
    val link: String? = null,
    val math: Boolean = false
)

internal sealed interface MarkdownBlock {
    data class Paragraph(val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Heading(val level: Int, val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Code(val language: String, val content: String) : MarkdownBlock
    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock
    data class ListBlock(val ordered: Boolean, val items: List<List<MarkdownBlock>>) : MarkdownBlock
    data class Table(val headers: List<List<MarkdownSpan>>, val rows: List<List<List<MarkdownSpan>>>) : MarkdownBlock
    data class Math(val latex: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

internal data class MarkdownRenderOptions(
    val fontScale: Float = 1.0f,
    val codeBlockAutoWrap: Boolean = false,
    val enableChineseTypography: Boolean = false
)

internal object DesktopMarkdownParser {
    private val flavour by lazy { GFMFlavourDescriptor(makeHttpsAutoLinks = true, useSafeLinks = true) }
    private val parser by lazy { MarkdownParser(flavour) }

    fun parse(content: String): List<MarkdownBlock> {
        if (content.isBlank()) return emptyList()
        val blockMath = Regex("(?s)\\$\\$(.+?)\\$\\$|\\\\\\[(.+?)\\\\\\]")
        val mathParts = blockMath.split(content)
        if (mathParts.size > 1) {
            val formulas = blockMath.findAll(content).iterator()
            return mathParts.flatMapIndexed { index, part ->
                buildList {
                    if (part.isNotBlank()) addAll(parse(part))
                    if (index < mathParts.lastIndex) {
                        val match = formulas.next()
                        add(MarkdownBlock.Math(match.groupValues[1].ifBlank { match.groupValues[2] }.trim()))
                    }
                }
            }
        }
        val root = parser.buildMarkdownTreeFromString(content)
        return parseChildren(root.children, content)
    }

    private fun parseChildren(nodes: List<ASTNode>, content: String): List<MarkdownBlock> = buildList {
        nodes.forEach { node -> parseBlock(node, content)?.let(::add) }
    }

    private fun parseBlock(node: ASTNode, content: String): MarkdownBlock? = when (node.type) {
        MarkdownElementTypes.PARAGRAPH -> MarkdownBlock.Paragraph(parseInline(node, content))
        MarkdownElementTypes.ATX_1 -> heading(node, content, 1)
        MarkdownElementTypes.ATX_2 -> heading(node, content, 2)
        MarkdownElementTypes.ATX_3 -> heading(node, content, 3)
        MarkdownElementTypes.ATX_4 -> heading(node, content, 4)
        MarkdownElementTypes.ATX_5 -> heading(node, content, 5)
        MarkdownElementTypes.ATX_6 -> heading(node, content, 6)
        MarkdownElementTypes.CODE_FENCE -> parseCodeFence(node, content)
        MarkdownElementTypes.CODE_BLOCK -> MarkdownBlock.Code("", node.getTextInNode(content).trimEnd())
        MarkdownElementTypes.BLOCK_QUOTE -> MarkdownBlock.Quote(parseChildren(node.children, content))
        MarkdownElementTypes.UNORDERED_LIST -> parseList(node, content, false)
        MarkdownElementTypes.ORDERED_LIST -> parseList(node, content, true)
        MarkdownTokenTypes.HORIZONTAL_RULE -> MarkdownBlock.Rule
        GFMElementTypes.TABLE -> parseTable(node, content)
        else -> null
    }

    private fun heading(node: ASTNode, content: String, level: Int): MarkdownBlock.Heading {
        val body = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT } ?: node
        return MarkdownBlock.Heading(level, parseInline(body, content).trimWhitespace())
    }

    private fun parseCodeFence(node: ASTNode, content: String): MarkdownBlock.Code {
        val language = node.findFirst(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content)?.trim().orEmpty()
        val contentNodes = node.children.filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
        val code = if (contentNodes.isEmpty()) {
            ""
        } else {
            content.substring(contentNodes.first().startOffset, contentNodes.last().endOffset).trimEnd()
        }
        return MarkdownBlock.Code(language, code)
    }

    private fun parseList(node: ASTNode, content: String, ordered: Boolean): MarkdownBlock.ListBlock {
        val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }.map { item ->
            parseChildren(item.children, content).ifEmpty {
                val spans = parseInline(item, content).filterNot { span ->
                    span.text.trim().matches(Regex("(?:[-+*]|\\d+\\.)"))
                }
                listOf(MarkdownBlock.Paragraph(spans.trimWhitespace()))
            }
        }
        return MarkdownBlock.ListBlock(ordered, items)
    }

    private fun parseTable(node: ASTNode, content: String): MarkdownBlock.Table? {
        fun cells(row: ASTNode): List<List<MarkdownSpan>> = row.children
            .filter { it.type == GFMTokenTypes.CELL }
            .map { parseInline(it, content).trimWhitespace() }

        val header = node.children.firstOrNull { it.type == GFMElementTypes.HEADER }?.let(::cells).orEmpty()
        if (header.isEmpty()) return null
        val rows = node.children.filter { it.type == GFMElementTypes.ROW }.map(::cells)
        return MarkdownBlock.Table(header, rows)
    }

    private data class InlineStyle(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val code: Boolean = false,
        val strikethrough: Boolean = false,
        val link: String? = null
    )

    private fun parseInline(node: ASTNode, content: String): List<MarkdownSpan> {
        val result = mutableListOf<MarkdownSpan>()

        fun append(text: String, style: InlineStyle) {
            if (text.isEmpty()) return
            val span = MarkdownSpan(
                text = text,
                bold = style.bold,
                italic = style.italic,
                code = style.code,
                strikethrough = style.strikethrough,
                link = style.link
            )
            val previous = result.lastOrNull()
            if (previous != null && previous.copy(text = "") == span.copy(text = "")) {
                result[result.lastIndex] = previous.copy(text = previous.text + text)
            } else {
                result += span
            }
        }

        fun visit(current: ASTNode, style: InlineStyle) {
            when (current.type) {
                MarkdownElementTypes.EMPH -> current.children.forEach { visit(it, style.copy(italic = true)) }
                MarkdownElementTypes.STRONG -> current.children.forEach { visit(it, style.copy(bold = true)) }
                GFMElementTypes.STRIKETHROUGH -> {
                    current.children.forEach { visit(it, style.copy(strikethrough = true)) }
                }
                MarkdownElementTypes.CODE_SPAN -> {
                    append(current.getTextInNode(content).trim('`').trim(), style.copy(code = true))
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    val destination = current.findFirst(MarkdownElementTypes.LINK_DESTINATION)
                        ?.getTextInNode(content)?.trim()?.trim('<', '>').orEmpty()
                    val label = current.findFirst(MarkdownElementTypes.LINK_TEXT)
                        ?.getTextInNode(content)?.trim()?.removeSurrounding("[", "]")
                        ?: destination
                    append(label, style.copy(link = destination.takeIf { it.isNotBlank() }))
                }
                MarkdownElementTypes.IMAGE -> {
                    val alt = current.findFirst(MarkdownElementTypes.LINK_TEXT)
                        ?.getTextInNode(content)?.trim()?.removeSurrounding("[", "]").orEmpty()
                    append(if (alt.isBlank()) "[Image]" else "[Image: $alt]", style.copy(italic = true))
                }
                GFMTokenTypes.GFM_AUTOLINK -> {
                    val link = current.getTextInNode(content).trim().trim('<', '>')
                    append(link, style.copy(link = link))
                }
                else -> {
                    if (current is LeafASTNode) {
                        if (current.type != MarkdownTokenTypes.EMPH && current.type != GFMTokenTypes.TILDE) {
                            append(current.getTextInNode(content), style)
                        }
                    } else {
                        current.children.forEach { visit(it, style) }
                    }
                }
            }
        }

        node.children.forEach { visit(it, InlineStyle()) }
        return result.flatMap { it.splitInlineMath() }
    }

    private fun ASTNode.findFirst(type: org.intellij.markdown.IElementType): ASTNode? {
        if (this.type == type) return this
        return children.firstNotNullOfOrNull { it.findFirst(type) }
    }

    private fun ASTNode.getTextInNode(text: String): String = text.substring(startOffset, endOffset)

    private fun List<MarkdownSpan>.trimWhitespace(): List<MarkdownSpan> {
        if (isEmpty()) return this
        return mapIndexed { index, span ->
            when (index) {
                0 -> span.copy(text = span.text.trimStart())
                lastIndex -> span.copy(text = span.text.trimEnd())
                else -> span
            }
        }.filter { it.text.isNotEmpty() }
    }
}

private fun MarkdownSpan.splitInlineMath(): List<MarkdownSpan> {
    if (code || text.isBlank()) return listOf(this)
    val matches = Regex("\\\\\\((.+?)\\\\\\)|\\$([^$\\n]+)\\$").findAll(text).toList()
    if (matches.isEmpty()) return listOf(this)
    var cursor = 0
    return buildList {
        matches.forEach { match ->
            if (match.range.first > cursor) add(copy(text = text.substring(cursor, match.range.first)))
            val latex = match.groupValues[1].ifBlank { match.groupValues[2] }.trim()
            add(if (latex.isBlank()) copy(text = match.value) else copy(text = latex, math = true))
            cursor = match.range.last + 1
        }
        if (cursor < text.length) add(copy(text = text.substring(cursor)))
    }
}

@Composable
internal fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    options: MarkdownRenderOptions = MarkdownRenderOptions()
) {
    val blocks = remember(content) { DesktopMarkdownParser.parse(content) }
    SelectionContainer {
        Column(
            modifier.animateContentSize(tween(180, easing = FastOutSlowInEasing)),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            blocks.forEach { MarkdownBlockView(it, options) }
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, options: MarkdownRenderOptions) {
    when (block) {
        is MarkdownBlock.Paragraph -> MarkdownText(
            block.spans,
            fontSize = 15.sp * options.fontScale,
            enableChineseTypography = options.enableChineseTypography
        )
        is MarkdownBlock.Heading -> {
            val fontSize = when (block.level) {
                1 -> 25.sp
                2 -> 22.sp
                3 -> 19.sp
                4 -> 17.sp
                else -> 15.sp
            }
            MarkdownText(
                block.spans.map { it.copy(bold = true) },
                modifier = Modifier.padding(top = if (block.level <= 2) 8.dp else 4.dp),
                fontSize = fontSize * options.fontScale,
                enableChineseTypography = options.enableChineseTypography
            )
        }
        is MarkdownBlock.Code -> CodeBlock(block, options)
        is MarkdownBlock.Quote -> {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    Modifier.fillMaxHeight().width(3.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                )
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    block.blocks.forEach { MarkdownBlockView(it, options) }
                }
            }
        }
        is MarkdownBlock.ListBlock -> {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row {
                        Text(
                            if (block.ordered) "${index + 1}." else "•",
                            modifier = Modifier.widthIn(min = 26.dp),
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 23.sp
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            item.forEach { MarkdownBlockView(it, options) }
                        }
                    }
                }
            }
        }
        is MarkdownBlock.Table -> MarkdownTable(block, options)
        is MarkdownBlock.Math -> LatexFormula(block.latex, options.fontScale)
        MarkdownBlock.Rule -> HorizontalDivider(
            Modifier.padding(vertical = 7.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun LatexFormula(latex: String, fontScale: Float) {
    val formulaColor = MaterialTheme.colorScheme.onSurface
    val bitmap = remember(latex, fontScale, formulaColor) {
        runCatching {
            val normalizedLatex = latex.trim().let { source ->
                if ('\n' in source && !source.contains("\\begin{")) {
                    "\\begin{aligned}${source.lines().joinToString("\\\\")}\\end{aligned}"
                } else {
                    source
                }
            }
            val icon = TeXFormula(normalizedLatex).createTeXIcon(TeXConstants.STYLE_DISPLAY, 18f * fontScale)
            val awtFormulaColor = java.awt.Color(
                formulaColor.red,
                formulaColor.green,
                formulaColor.blue,
                formulaColor.alpha
            )
            icon.setForeground(awtFormulaColor)
            val image = BufferedImage(icon.iconWidth.coerceAtLeast(1), icon.iconHeight.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try {
                graphics.color = awtFormulaColor
                icon.paintIcon(null, graphics, 0, 0)
            } finally {
                graphics.dispose()
            }
            ByteArrayOutputStream().use { output ->
                ImageIO.write(image, "png", output)
                org.jetbrains.skia.Image.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, "数学公式", Modifier.padding(vertical = 4.dp))
    } else {
        Text(latex, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun MarkdownText(
    spans: List<MarkdownSpan>,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    fillWidth: Boolean = true,
    enableChineseTypography: Boolean = false
) {
    val displaySpans = remember(spans, enableChineseTypography) {
        spans.withChineseTypography(enableChineseTypography)
    }
    if (displaySpans.any { it.math }) {
        FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.Center) {
            displaySpans.forEach { span ->
                if (span.math) LatexFormula(span.text, fontSize.value / 15f)
                else MarkdownText(
                    listOf(span),
                    fontSize = fontSize,
                    fillWidth = false,
                    enableChineseTypography = false
                )
            }
        }
        return
    }
    if (displaySpans.any { it.code }) {
        FlowRow(
            modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.Center
        ) {
            displaySpans.forEach { span ->
                if (span.code) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            span.text,
                            Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
                            lineHeight = fontSize * 1.53f
                        )
                    }
                } else {
                    MarkdownText(
                        spans = listOf(span),
                        fontSize = fontSize,
                        fillWidth = false,
                        enableChineseTypography = false
                    )
                }
            }
        }
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val annotated = remember(displaySpans, primary, codeBackground) {
        buildAnnotatedString {
            displaySpans.forEach { span ->
                val start = length
                append(span.text)
                addStyle(
                    SpanStyle(
                        fontWeight = if (span.bold) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        fontFamily = if (span.code) FontFamily.Monospace else null,
                        background = if (span.code) codeBackground else Color.Unspecified,
                        color = if (span.link != null) primary else Color.Unspecified,
                        textDecoration = when {
                            span.strikethrough -> TextDecoration.LineThrough
                            span.link != null -> TextDecoration.Underline
                            else -> null
                        }
                    ),
                    start,
                    length
                )
                span.link?.let { addLink(LinkAnnotation.Url(it), start, length) }
            }
        }
    }
    Text(
        text = annotated,
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize, lineHeight = fontSize * 1.53f)
    )
}

internal fun List<MarkdownSpan>.withChineseTypography(enabled: Boolean): List<MarkdownSpan> =
    if (!enabled) this else map { span ->
        if (span.code || span.math) span else span.copy(text = span.text.spacingText())
    }

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun CodeBlock(block: MarkdownBlock.Code, options: MarkdownRenderOptions) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copyVersion by remember(block.content) { mutableStateOf(0) }
    var copied by remember(block.content) { mutableStateOf(false) }
    LaunchedEffect(copyVersion) {
        if (copyVersion > 0) {
            copied = true
            delay(1_000)
            copied = false
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(start = 12.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    block.language.ifBlank { "Code" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                androidx.compose.material3.IconButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(StringSelection(block.content)))
                            copyVersion++
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    AnimatedContent(
                        targetState = if (copied) Lucide.Check else Lucide.Copy,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "codeCopyIcon"
                    ) { icon ->
                        androidx.compose.material3.Icon(icon, "复制代码", Modifier.size(16.dp))
                    }
                }
            }
            Text(
                highlightedCode(block.content, block.language),
                if (options.codeBlockAutoWrap) {
                    Modifier.fillMaxWidth().padding(12.dp)
                } else {
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp)
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp * options.fontScale,
                lineHeight = 19.sp * options.fontScale,
                softWrap = options.codeBlockAutoWrap
            )
        }
    }
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table, options: MarkdownRenderOptions) {
    val columnCount = maxOf(table.headers.size, table.rows.maxOfOrNull { it.size } ?: 0)
    val shape = RoundedCornerShape(10.dp)
    val scrollState = rememberScrollState()
    val edgeColor = MaterialTheme.colorScheme.background
    val canScrollBackward = scrollState.value > 0
    val canScrollForward = scrollState.value < scrollState.maxValue

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                Column(
                    Modifier.width((columnCount * 160).dp).clip(shape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                ) {
                    TableRow(table.headers, columnCount, header = true, options = options)
                    table.rows.forEach { TableRow(it, columnCount, header = false, options = options) }
                }
            }
            if (canScrollBackward) {
                Box(
                    Modifier.matchParentSize()
                ) {
                    Box(
                        Modifier.align(Alignment.CenterStart).width(48.dp).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(edgeColor, edgeColor.copy(alpha = 0f))))
                    )
                }
            }
            if (canScrollForward) {
                Box(
                    Modifier.matchParentSize()
                ) {
                    Box(
                        Modifier.align(Alignment.CenterEnd).width(48.dp).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(edgeColor.copy(alpha = 0f), edgeColor)))
                    )
                }
            }
        }
        if (scrollState.maxValue > 0) {
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TableRow(
    cells: List<List<MarkdownSpan>>,
    columnCount: Int,
    header: Boolean,
    options: MarkdownRenderOptions
) {
    Row(
        Modifier.height(IntrinsicSize.Min)
            .background(if (header) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
    ) {
        repeat(columnCount) { index ->
            MarkdownText(
                spans = cells.getOrElse(index) { emptyList() }.map { span ->
                    span.copy(bold = header || span.bold)
                },
                Modifier.width(160.dp).fillMaxHeight().border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ).padding(horizontal = 10.dp, vertical = 8.dp),
                fontSize = 13.sp * options.fontScale,
                fillWidth = false,
                enableChineseTypography = options.enableChineseTypography
            )
        }
    }
}
