package me.rerere.rikkahub.desktop

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopAttachmentsTest {
    @Test
    fun loadsTextAndImageAttachments() {
        val directory = Files.createTempDirectory("rikkahub-attachments")
        val text = directory.resolve("notes.md")
        val image = directory.resolve("photo.png")
        Files.writeString(text, "hello")
        Files.write(image, byteArrayOf(1, 2, 3))

        val textAttachment = loadDesktopAttachment(text.toFile())
        val imageAttachment = loadDesktopAttachment(image.toFile())

        assertEquals("hello", textAttachment.data)
        assertEquals(false, textAttachment.isImage)
        assertEquals("image/png", imageAttachment.mimeType)
        assertTrue(imageAttachment.isImage)
        assertEquals("AQID", imageAttachment.data)
    }

    @Test
    fun loadsMp3AndWavAttachmentsForMultimodalRequests() {
        val directory = Files.createTempDirectory("rikkahub-audio-attachments")
        val mp3 = directory.resolve("voice.mp3")
        val wav = directory.resolve("voice.wav")
        Files.write(mp3, byteArrayOf(1, 2, 3))
        Files.write(wav, byteArrayOf(4, 5, 6))

        val mp3Attachment = loadDesktopAttachment(mp3.toFile())
        val wavAttachment = loadDesktopAttachment(wav.toFile())

        assertEquals(DesktopAttachmentKind.AUDIO, mp3Attachment.kind)
        assertEquals("audio/mpeg", mp3Attachment.mimeType)
        assertEquals("AQID", mp3Attachment.data)
        assertEquals(DesktopAttachmentKind.AUDIO, wavAttachment.kind)
        assertEquals("audio/wav", wavAttachment.mimeType)
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
        assertTrue(loadDesktopAttachment(docx.toFile()).data.contains("DOCX text"))
        assertTrue(loadDesktopAttachment(pptx.toFile()).data.contains("Slide text"))
        assertTrue(loadDesktopAttachment(epub.toFile()).data.contains("EPUB text"))
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
}
