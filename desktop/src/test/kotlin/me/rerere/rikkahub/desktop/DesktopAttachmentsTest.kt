package me.rerere.rikkahub.desktop

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.nio.file.Files
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopAttachmentsTest {
    @Test
    fun loadsTextAndImageAttachments() {
        val directory = Files.createTempDirectory("rikkahub-attachments")
        val text = directory.resolve("notes.md")
        val image = directory.resolve("photo.png")
        Files.writeString(text, "hello")
        Files.write(image, Base64.getDecoder().decode(OnePixelPngBase64))

        val textAttachment = loadDesktopAttachment(text.toFile())
        val imageAttachment = loadDesktopAttachment(image.toFile())

        assertEquals("hello", textAttachment.data)
        assertEquals(false, textAttachment.isImage)
        assertEquals("image/png", imageAttachment.mimeType)
        assertTrue(imageAttachment.isImage)
        assertEquals(OnePixelPngBase64, imageAttachment.data)
        assertEquals(1, imageAttachment.imageWidth)
        assertEquals(1, imageAttachment.imageHeight)
    }

    @Test
    fun loadsSupportedAudioAttachmentsForMultimodalRequests() {
        val directory = Files.createTempDirectory("rikkahub-audio-attachments")
        val expected = mapOf(
            "mp3" to ("audio/mpeg" to "mp3"),
            "wav" to ("audio/wav" to "wav"),
            "m4a" to ("audio/mp4" to "m4a"),
            "aac" to ("audio/aac" to "aac"),
            "flac" to ("audio/flac" to "flac"),
            "ogg" to ("audio/ogg" to "ogg"),
            "opus" to ("audio/opus" to "opus")
        )

        expected.forEach { (extension, expectedType) ->
            val file = directory.resolve("voice.$extension")
            Files.write(file, byteArrayOf(1, 2, 3))
            val attachment = loadDesktopAttachment(file.toFile())

            assertEquals(DesktopAttachmentKind.AUDIO, attachment.kind)
            assertEquals(expectedType.first, attachment.mimeType)
            assertEquals(expectedType.second, attachment.audioFormat)
            assertEquals("AQID", attachment.data)
            assertTrue(isDesktopAttachmentSupported(file.toFile()))
        }
    }

    @Test
    fun rejectsImageWhoseContentDoesNotMatchItsExtension() {
        val image = Files.createTempFile("rikkahub-mismatched-image", ".jpg")
        Files.write(image, Base64.getDecoder().decode(OnePixelPngBase64))

        val error = assertFailsWith<IllegalArgumentException> { loadDesktopAttachment(image.toFile()) }

        assertTrue(error.message.orEmpty().contains("does not match"))
    }

    @Test
    fun rejectsCorruptImageAttachments() {
        val image = Files.createTempFile("rikkahub-corrupt-image", ".png")
        Files.write(image, byteArrayOf(1, 2, 3))

        assertFailsWith<IllegalArgumentException> { loadDesktopAttachment(image.toFile()) }
    }

    @Test
    fun deduplicatesExactAttachmentsWithoutMergingDifferentOriginalDocuments() {
        val first = DesktopAttachment(
            "report.pdf",
            "text/plain",
            "same extracted text",
            rawData = "FIRST",
            rawMimeType = "application/pdf"
        )
        val second = first.copy(rawData = "SECOND")

        val result = deduplicateDesktopAttachments(listOf(first, first, second))

        assertEquals(listOf(first, second), result)
    }

    @Test
    fun rejectsUnsupportedFiles() {
        val file = Files.createTempFile("rikkahub-attachment", ".bin")
        Files.write(file, byteArrayOf(1, 2, 3))

        assertFailsWith<IllegalArgumentException> { loadDesktopAttachment(file.toFile()) }
    }

    @Test
    fun extractsTextFromOfficeAndEpubAttachments() {
        val directory = Files.createTempDirectory("rikkahub-documents")
        val docx = directory.resolve("notes.docx")
        val pptx = directory.resolve("slides.pptx")
        val epub = directory.resolve("book.epub")
        writeZip(docx, "word/document.xml" to "<document><body><p><r><t>DOCX text</t></r></p></body></document>")
        writeZip(pptx, "ppt/slides/slide1.xml" to "<sld><sp><p><r><t>Slide text</t></r></p></sp></sld>")
        writeZip(epub, "OEBPS/chapter.xhtml" to "<html><body><h1>Chapter</h1><p>EPUB text</p></body></html>")

        assertTrue(isDesktopAttachmentSupported(docx.toFile()))
        val docxAttachment = loadDesktopAttachment(docx.toFile())
        val pptxAttachment = loadDesktopAttachment(pptx.toFile())
        val epubAttachment = loadDesktopAttachment(epub.toFile())
        assertTrue(docxAttachment.data.contains("DOCX text"))
        assertTrue(pptxAttachment.data.contains("Slide text"))
        assertTrue(epubAttachment.data.contains("EPUB text"))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxAttachment.rawMimeType
        )
        assertNotNull(docxAttachment.rawData)
        assertNotNull(pptxAttachment.rawData)
        assertNotNull(epubAttachment.rawData)
    }

    @Test
    fun extractsTextFromPdfAttachments() {
        val pdf = Files.createTempFile("rikkahub-document", ".pdf")
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                content.newLineAtOffset(72f, 720f)
                content.showText("PDF attachment text")
                content.endText()
            }
            document.save(pdf.toFile())
        }

        assertTrue(isDesktopAttachmentSupported(pdf.toFile()))
        val attachment = loadDesktopAttachment(pdf.toFile())
        assertEquals("text/plain", attachment.mimeType)
        assertEquals("application/pdf", attachment.rawMimeType)
        assertNotNull(attachment.rawData)
        assertEquals(Files.size(pdf), attachment.sizeBytes)
        assertTrue(attachment.data.contains("PDF attachment text"))
    }

    private fun writeZip(path: java.nio.file.Path, vararg entries: Pair<String, String>) {
        ZipOutputStream(Files.newOutputStream(path)).use { output ->
            entries.forEach { (name, content) ->
                output.putNextEntry(ZipEntry(name))
                output.write(content.encodeToByteArray())
                output.closeEntry()
            }
        }
    }

    private companion object {
        const val OnePixelPngBase64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    }
}
