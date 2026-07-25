package me.rerere.rikkahub.desktop

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper

private const val MaxDocumentTextCharacters = 200_000
private const val MaxDocumentXmlBytes = 5_000_000
private const val MaxPdfPages = 200

/**
 * Minimal, local-only extraction for XML-based office formats. PDF requires a desktop-native
 * renderer/extractor and is intentionally not advertised until one is available.
 */
internal fun extractDesktopDocumentText(file: File): String = when (file.extension.lowercase()) {
    "pdf" -> extractPdfText(file)
    "docx" -> extractDocxText(file)
    "pptx" -> extractPptxText(file)
    "epub" -> extractEpubText(file)
    else -> error("Unsupported document type: ${file.name}")
}

private fun extractPdfText(file: File): String = Loader.loadPDF(file).use { document ->
    val reachedPageLimit = document.numberOfPages > MaxPdfPages
    val text = PDFTextStripper().apply {
        startPage = 1
        endPage = minOf(document.numberOfPages, MaxPdfPages)
    }.getText(document).trim()
    val truncated = text.length > MaxDocumentTextCharacters || reachedPageLimit
    text.take(MaxDocumentTextCharacters)
        .plus(if (truncated) "\n\n[文档内容已截断]" else "")
        .ifBlank { error("PDF 中没有可读取的文本") }
}

private fun extractDocxText(file: File): String = ZipFile(file).use { zip ->
    val entry = zip.getEntry("word/document.xml") ?: error("DOCX 内容缺失")
    zip.getInputStream(entry).use { extractXmlText(it.readDocumentXml(), paragraphTags = setOf("p")) }
        .ifBlank { error("DOCX 中没有可读取的文本") }
}

private fun extractPptxText(file: File): String = ZipFile(file).use { zip ->
    zip.entries().asSequence()
        .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
        .sortedBy { it.name.substringAfter("slide").substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE }
        .mapIndexed { index, entry ->
            val text = zip.getInputStream(entry).use { extractXmlText(it.readDocumentXml(), paragraphTags = setOf("p")) }
            "## 幻灯片 ${index + 1}\n\n$text"
        }
        .joinToString("\n\n")
        .ifBlank { error("PPTX 中没有可读取的幻灯片") }
}

private fun extractEpubText(file: File): String = ZipFile(file).use { zip ->
    zip.entries().asSequence()
        .filter { entry -> entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".html", true) }
        .filterNot { it.name.contains("META-INF", true) }
        .sortedBy { it.name }
        .map { entry -> zip.getInputStream(entry).use { extractXmlText(it.readDocumentXml(), paragraphTags = setOf("p", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6")) } }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
        .ifBlank { error("EPUB 中没有可读取的文本") }
}

private fun extractXmlText(bytes: ByteArray, paragraphTags: Set<String>): String {
    val factory = XMLInputFactory.newFactory().apply {
        setPropertyIfSupported(XMLInputFactory.SUPPORT_DTD, false)
        setPropertyIfSupported("javax.xml.stream.isSupportingExternalEntities", false)
    }
    val output = StringBuilder()
    val reader = factory.createXMLStreamReader(bytes.inputStream())
    try {
        while (reader.hasNext() && output.length < MaxDocumentTextCharacters) {
            when (reader.next()) {
                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> output.append(reader.text)
                XMLStreamConstants.END_ELEMENT -> if (reader.localName in paragraphTags) output.append("\n\n")
            }
        }
    } finally {
        reader.close()
    }
    return output.toString()
        .replace(Regex("[\\t ]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
        .let { if (output.length >= MaxDocumentTextCharacters) "$it\n\n[文档内容已截断]" else it }
}

private fun XMLInputFactory.setPropertyIfSupported(name: String, value: Any) {
    runCatching { setProperty(name, value) }
}

private fun InputStream.readDocumentXml(): ByteArray {
    val bytes = readNBytes(MaxDocumentXmlBytes + 1)
    require(bytes.size <= MaxDocumentXmlBytes) { "文档 XML 条目过大" }
    return bytes
}
