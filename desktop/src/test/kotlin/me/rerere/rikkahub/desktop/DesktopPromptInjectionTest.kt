package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPromptInjectionTest {
    @Test
    fun injectsTriggeredEntriesAtTheirConfiguredLocations() {
        val assistant = DesktopAssistantProfile(
            promptInjections = listOf(
                DesktopPromptInjection(
                    name = "Prefix",
                    content = "prefix",
                    position = DesktopInjectionPosition.BEFORE_SYSTEM_PROMPT,
                    constantActive = true
                ),
                DesktopPromptInjection(
                    name = "World entry",
                    content = "remember dragons",
                    role = "system",
                    position = DesktopInjectionPosition.BOTTOM_OF_CHAT,
                    keywords = listOf("dragon")
                ),
                DesktopPromptInjection(
                    name = "Ignored",
                    content = "ignore me",
                    keywords = listOf("unmatched")
                )
            )
        )

        val injected = assistant.injectPromptMessages(
            listOf(ChatMessage("user", "Tell me about a dragon"), ChatMessage("assistant", "Sure"))
        )

        assertEquals(listOf("prefix"), injected.systemPrefix)
        assertEquals(listOf("Tell me about a dragon", "remember dragons", "Sure"), injected.messages.map { it.content })
        assertFalse(injected.messages.any { it.content == "ignore me" })
    }

    @Test
    fun honorsRegexCaseAndScanDepth() {
        val assistant = DesktopAssistantProfile(
            promptInjections = listOf(
                DesktopPromptInjection(
                    content = "matched",
                    position = DesktopInjectionPosition.TOP_OF_CHAT,
                    keywords = listOf("HELLO\\d+"),
                    useRegex = true,
                    caseSensitive = false,
                    scanDepth = 1
                )
            )
        )

        val injected = assistant.injectPromptMessages(
            listOf(ChatMessage("user", "old hello7"), ChatMessage("user", "new hello8"))
        )

        assertTrue(injected.messages.first().content == "matched")
    }

    @Test
    fun appliesHigherPriorityEntriesFirstAtTheSameLocation() {
        val assistant = DesktopAssistantProfile(
            promptInjections = listOf(
                DesktopPromptInjection(
                    content = "low priority",
                    position = DesktopInjectionPosition.TOP_OF_CHAT,
                    priority = 1,
                    constantActive = true
                ),
                DesktopPromptInjection(
                    content = "high priority",
                    position = DesktopInjectionPosition.TOP_OF_CHAT,
                    priority = 10,
                    constantActive = true
                )
            )
        )

        val injected = assistant.injectPromptMessages(listOf(ChatMessage("user", "hello")))

        assertEquals(listOf("high priority", "low priority", "hello"), injected.messages.map { it.content })
    }

    @Test
    fun compressionPreservesRecoverableHistoryAndRecentMessages() {
        val conversation = DesktopConversation(
            messages = listOf(
                ChatMessage("user", "old question"),
                ChatMessage("assistant", "old answer"),
                ChatMessage("user", "recent question")
            )
        )

        val compressed = conversation.replaceHistoryWithSummary("important old context", keepRecentMessages = 1)

        assertEquals(
            listOf("[历史对话摘要]\nimportant old context", "recent question"),
            compressed.messages.map { it.content }
        )
        assertEquals(conversation.messages, compressed.branches.single().messages)
        assertEquals(
            "USER:\nold question\n\nASSISTANT:\nold answer",
            conversation.messages.dropLast(1).compressionTranscript()
        )
    }

    @Test
    fun conversationCanDisableAssistantPromptInjectionsWhenAllowed() {
        val assistant = DesktopAssistantProfile(allowConversationPromptInjection = true)

        assertTrue(DesktopConversation().usesPromptInjections(assistant))
        assertFalse(DesktopConversation(promptInjectionsEnabled = false).usesPromptInjections(assistant))
        assertTrue(DesktopConversation(promptInjectionsEnabled = true).usesPromptInjections(assistant))
        assertTrue(
            DesktopConversation(promptInjectionsEnabled = false).usesPromptInjections(
                DesktopAssistantProfile(allowConversationPromptInjection = false)
            )
        )
    }
}
