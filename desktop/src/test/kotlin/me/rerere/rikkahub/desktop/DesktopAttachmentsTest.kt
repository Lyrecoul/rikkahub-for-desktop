package me.rerere.rikkahub.desktop

import java.nio.file.Files
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
    fun rejectsUnsupportedFiles() {
        val file = Files.createTempFile("rikkahub-attachment", ".bin")
        Files.write(file, byteArrayOf(1, 2, 3))

        assertFailsWith<IllegalArgumentException> { loadDesktopAttachment(file.toFile()) }
    }
}
