package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopModelCapabilitiesTest {
    @Test
    fun infersImageAudioReasoningAndToolCapabilities() {
        val capabilities = inferDesktopModelCapabilities("gpt-4o-audio-preview")

        assertTrue(DesktopModality.TEXT in capabilities.inputModalities)
        assertTrue(DesktopModality.IMAGE in capabilities.inputModalities)
        assertTrue(DesktopModality.AUDIO in capabilities.inputModalities)
        assertTrue(DesktopModality.DOCUMENT in capabilities.inputModalities)
        assertTrue(capabilities.supportsTools)
        assertEquals(setOf("mp3", "wav"), capabilities.acceptedAudioFormats)
    }

    @Test
    fun rejectsImageAndAudioForUnknownTextModel() {
        val messages = listOf(
            ChatMessage(
                role = "user",
                content = "inspect",
                attachments = listOf(
                    DesktopAttachment("photo.png", "image/png", "AQID", isImage = true),
                    DesktopAttachment(
                        "voice.wav",
                        "audio/wav",
                        "BAUG",
                        kind = DesktopAttachmentKind.AUDIO
                    )
                )
            )
        )

        val issues = DesktopConfig(model = "custom-text-model").validateAttachments(messages)

        assertEquals(listOf("photo.png", "voice.wav"), issues.map { it.attachmentName })
    }

    @Test
    fun restrictsExtendedAudioFormatsToGeminiProtocolAndCapabilities() {
        val audio = DesktopAttachment(
            "voice.m4a",
            "audio/mp4",
            "AQID",
            kind = DesktopAttachmentKind.AUDIO,
            audioFormat = "m4a"
        )
        val openAi = DesktopConfig(model = "gpt-4o-audio-preview")
        val gemini = DesktopConfig(
            protocol = DesktopProviderProtocol.GEMINI_GENERATE_CONTENT,
            model = "gemini-2.5-flash"
        )

        assertTrue(openAi.validateAttachments(listOf(ChatMessage("user", "", attachments = listOf(audio))))
            .single().reason.contains("m4a"))
        assertTrue(gemini.validateAttachments(listOf(ChatMessage("user", "", attachments = listOf(audio)))).isEmpty())
        assertTrue("m4a" in gemini.modelCapabilities().acceptedAudioFormats)
    }

    @Test
    fun allowsKnownVisionModelAndLocallyExtractedDocument() {
        val messages = listOf(
            ChatMessage(
                role = "user",
                content = "inspect",
                attachments = listOf(
                    DesktopAttachment("photo.webp", "image/webp", "AQID", isImage = true),
                    DesktopAttachment("notes.pdf", "text/plain", "extracted text")
                )
            )
        )

        assertTrue(DesktopConfig(model = "gpt-4.1-mini").validateAttachments(messages).isEmpty())
    }

    @Test
    fun manualCapabilityOverrideTakesPriorityOverInference() {
        val model = "private-multimodal-model"
        val override = DesktopModelCapabilities(
            inputModalities = setOf(DesktopModality.TEXT, DesktopModality.IMAGE),
            acceptedImageMimeTypes = setOf("image/png")
        )
        val config = DesktopConfig(model = model, modelCapabilityOverrides = mapOf(model to override))
        val message = ChatMessage(
            role = "user",
            content = "inspect",
            attachments = listOf(DesktopAttachment("photo.png", "image/png", "AQID", isImage = true))
        )

        assertEquals(override, config.modelCapabilities())
        assertTrue(config.validateAttachments(listOf(message)).isEmpty())
    }

    @Test
    fun rejectsImportedImageMetadataAbovePixelLimit() {
        val image = DesktopAttachment(
            "large.png",
            "image/png",
            "AQID",
            isImage = true,
            imageWidth = 10_000,
            imageHeight = 5_000
        )
        val config = DesktopConfig(model = "gpt-4.1-mini")

        val issue = config.validateAttachments(
            listOf(ChatMessage("user", "", attachments = listOf(image)))
        ).single()

        assertTrue(issue.reason.contains("40 megapixel"))
    }

    @Test
    fun validatesAggregateAttachmentSizeOverride() {
        val model = "limited-model"
        val config = DesktopConfig(
            model = model,
            modelCapabilityOverrides = mapOf(
                model to DesktopModelCapabilities(maxAttachmentBytes = 5, maxRequestBytes = 7)
            )
        )
        val messages = listOf(
            ChatMessage(
                role = "user",
                content = "inspect",
                attachments = listOf(
                    DesktopAttachment("first.txt", "text/plain", "text", sizeBytes = 4),
                    DesktopAttachment("second.txt", "text/plain", "text", sizeBytes = 4)
                )
            )
        )

        val issues = config.validateAttachments(messages)

        assertEquals(listOf("Request"), issues.map { it.attachmentName })
        assertTrue(issues.single().reason.contains("8 bytes"))
    }

    @Test
    fun resolvesOpenAiChatCompletionsAdapter() {
        val config = DesktopConfig(baseUrl = "https://example.com/v1/")
        val adapter = desktopChatProviderAdapter(config.protocol)

        assertEquals("https://example.com/v1/chat/completions", adapter.chatEndpoint(config))
        assertTrue(adapter.buildRequestBody(config, listOf(ChatMessage("user", "hello"))).contains("\"messages\""))
    }
}
